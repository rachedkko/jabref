package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultLatexParser;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatexCitationsTabViewModel extends AbstractViewModel {

    enum Status {
        IN_PROGRESS,
        CITATIONS_FOUND,
        NO_RESULTS,
        ERROR
    }

    enum Action {
        ADD,
        REMOVE,
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexCitationsTabViewModel.class);
    private static final String TEX_EXT = ".tex";
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final ObjectProperty<Path> directory;
    private final ObservableList<Citation> citationList;
    private final ObjectProperty<Status> status;
    private final StringProperty searchError;
    private Future<?> searchTask;
    private LatexParserResult latexParserResult;
    private BibEntry currentEntry;
    private final DefaultFileUpdateMonitor fileUpdateMonitor;
    private boolean hasListener;
    private final Map<Path, FileUpdateListener> listeners = new HashMap<>();

    public LatexCitationsTabViewModel(BibDatabaseContext databaseContext,
                                      PreferencesService preferencesService,
                                      TaskExecutor taskExecutor,
                                      DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.directory = new SimpleObjectProperty<>(databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                                                   .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory())));
        this.citationList = FXCollections.observableArrayList();
        this.status = new SimpleObjectProperty<>(Status.IN_PROGRESS);
        this.searchError = new SimpleStringProperty("");

        this.fileUpdateMonitor = (DefaultFileUpdateMonitor) Globals.getFileUpdateMonitor();
        this.hasListener = false;
    }

    public void init(BibEntry entry) {
        cancelSearch();

        currentEntry = entry;
        Optional<String> citeKey = entry.getCitationKey();

        if (citeKey.isPresent()) {
            startSearch(citeKey.get());
            updateListener(directory.get());
        } else {
            searchError.set(Localization.lang("Selected entry does not have an associated citation key."));
            status.set(Status.ERROR);
        }
    }

    public ObjectProperty<Path> directoryProperty() {
        return directory;
    }

    public ObservableList<Citation> getCitationList() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public StringProperty searchErrorProperty() {
        return searchError;
    }

    private void startSearch(String citeKey) {
        searchTask = BackgroundTask.wrap(() -> searchAndParse(citeKey))
                                   .onRunning(() -> status.set(Status.IN_PROGRESS))
                                   .onSuccess(result -> {
                                       citationList.setAll(result);
                                       status.set(citationList.isEmpty() ? Status.NO_RESULTS : Status.CITATIONS_FOUND);
                                   })
                                   .onFailure(error -> {
                                       searchError.set(error.getMessage());
                                       status.set(Status.ERROR);
                                   })
                                   .executeWith(taskExecutor);
    }

    private void cancelSearch() {
        if (searchTask == null || searchTask.isCancelled() || searchTask.isDone()) {
            return;
        }

        status.set(Status.IN_PROGRESS);
        searchTask.cancel(true);
    }

    private Collection<Citation> searchAndParse(String citeKey) throws IOException {
        // we need to check whether the user meanwhile set the LaTeX file directory or the database changed locations
        Path newDirectory = databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                                           .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory()));

        if (latexParserResult == null || !newDirectory.equals(directory.get())) {
            directory.set(newDirectory);

            if (!newDirectory.toFile().exists()) {
                throw new IOException("Current search directory does not exist: %s".formatted(newDirectory));
            }

            List<Path> texFiles = searchDirectory(newDirectory);
            LOGGER.debug("Found tex files: {}", texFiles);
            latexParserResult = new DefaultLatexParser().parse(texFiles);
        }

        return latexParserResult.getCitationsByKey(citeKey);
    }

    /**
     * @param directory the directory to search for. It is recursively searched.
     */
    private List<Path> searchDirectory(Path directory) {
        LOGGER.debug("Searching directory {}", directory);
        try {
            return Files.walk(directory)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(TEX_EXT))
                        .toList();
        } catch (IOException e) {
            LOGGER.error("Error while searching files", e);
            return List.of();
        }
    }

    private void updateListener(Path path) {
        if (!hasListener) {
            try {
                listenerOnDirectory(path, Action.ADD);
                hasListener = true;
            } catch (IOException e) {
                LOGGER.error("Could add listener", e);
            }
        }
    }

    private void updateListener(Path oldPath, Path newPath) {
        try {
            listenerOnDirectory(newPath, Action.ADD);
            listenerOnDirectory(oldPath, Action.REMOVE);
        } catch (IOException e) {
            LOGGER.error("Could not find file", e);
        }
    }

    private void listenerOnDirectory(Path path, Action action) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
             paths.filter(curPath -> !Files.isRegularFile(curPath))
                 .forEach(curDir -> {
                     try {
                         if (action == Action.ADD) {
                             FileUpdateListener newListener = this::refreshLatexDirectory;
                             listeners.put(curDir, newListener);
                             fileUpdateMonitor.addListenerForDirectory(curDir, newListener);
                         } else if (action == Action.REMOVE) {
                             FileUpdateListener oldListener = listeners.remove(curDir);
                             fileUpdateMonitor.removeListener(curDir, oldListener);
                         }
                     } catch (IOException e) {
                         LOGGER.error("Could not find file", e);
                     }
                 });
        } catch (IOException e) {
            LOGGER.error("Could not create a Stream<Path>", e);
        }
    }

    public void setLatexDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(directory.get()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration).ifPresent(selectedDirectory -> {
                    databaseContext.getMetaData().setLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost(), selectedDirectory.toAbsolutePath());
                    updateListener(directory.get(), selectedDirectory);
        });

        init(currentEntry);
    }

    public void refreshLatexDirectory() {
        latexParserResult = null;
        Platform.runLater(() -> {
            cancelSearch();
            init(currentEntry);
        });
    }

    public boolean shouldShow() {
        return preferencesService.getEntryEditorPreferences().shouldShowLatexCitationsTab();
    }

    public void shutdown() {
        // remove listeners
        if (hasListener) {
            try {
                listenerOnDirectory(directory.get(), Action.REMOVE);
                hasListener = false;
            } catch (IOException e) {
                LOGGER.error("Could not remove listener", e);
            }
        }
        fileUpdateMonitor.shutdown();
    }
}