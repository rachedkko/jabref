# Report for assignment 4

## Project

Name: JabRef

URL: https://github.com/JabRef/jabref, our fork : https://github.com/rachedkko/jabref/tree/main

JabRef is an open-source, cross-platform citation and reference management tool.
JabRef helps you to collect and organize sources, find the paper you need and discover the latest research.

## Onboarding experience

Choosing a project was a time-consuming task for us. First, we wanted to keep the project we worked on 
for assignment 3 (Scrapy), however we found that the issues seemed to difficult for us 
to resolve. Helped by the following list of open-source projects : https://github.com/MunGell/awesome-for-beginners. 

We had to search for a few hours to find a project with a reactive community, opened
to assign issues to beginners, and with good documentation. We finally found JabRef which met
all of our criteria. 

## Effort spent

For each team member, how much time was spent in

| /                                        | Roxanne | Alexander | Rached  | Iley    | Marcus |
|------------------------------------------|---------|-----------|---------|---------|--------|
| 1. plenary discussions/meetings          | 3h      |           | .....   | .....   | ....   |
| 2. discussions within parts of the group | 0.5h    | .         | ....    | ...     | ....   |
| 3. reading documentation                 | 3h      | .         | ....    | ...     | ....   |
| 4. configuration and setup               | 0.5h    | .         | ....    | ...     | ....   |
| 5. analyzing code/output                 | 2h      | .         | ....    | ...     | ....   |
| 6. writing documentation                 | 0h      | .         | ....    | ...     | ....   |
| 7. writing code                          | 6h      | .         | ....    | ...     | ....   |
| 8. running code                          | 2h      | .         | ....    | ...     | ....   |
| 9. Writing the report                    | 1h      | .         | ....    | ...     | ....   |
| **Total**                                | **18h** | .         | ....    | ...     | ....   |

 

For setting up tools and libraries (step 4), enumerate all dependencies
you took care of and where you spent your time, if that time exceeds
30 minutes.

## Overview of issue(s) and work done.

Title: Add FileMonitor for LaTeX citations

URL: https://github.com/JabRef/jabref/issues/10585 

Once you've created a library, you may click on one of your source. You can find a LaTeX citation tab, 
it will ask you to select one of your directories and will search in this directory whether there is 
one (or many) LaTeX file(s) citing the specific selected source. The issue is that if a file changes in this 
directory or if a file is created or deleted, the LaTeX file(s) shown to the user are not automatically 
reloaded. Our goal was to trigger a new search for LaTeX file(s) citing the source each time a change in the 
directory occurs.

The scope of this issue is not huge, it affects only the "LaTeX citation" tab. In the code, the main file we had to
look at was src/main/java/org/jabref/gui/entryeditor/LatexCitationsTabViewModel.java which is the model for this 
tab and contains all the logic about it, and
src/main/java/org/jabref/gui/util/DefaultFileUpdateMonitor.java which is a class implementing FileUpdateMonitor. 
The monitor of our directory was an instance of this DefaultFileUpdateMonitor class, and we had to alter the class a bit.  


## Requirements for the new feature or requirements affected by functionality being refactored

_TODO_ Non-optional : Requirements related to the functionality are identified and described in a systematic way. Each requirement has a name (ID), title, and description. The description can be one paragraph per require- ment.

Optional (point 3): trace tests to requirements.

## Code changes

### Patch

To see our changes you may go to the branch `latexcitation-filemonitor-issue-10585` and run the command `git diff d81192369eb499421c04588e4e0cb0993215607c`
which corresponds to the last commit before any of our changes. 

Optional (point 4): To check that our patch is clean, the easiest way to do so is by going in the Action tab of Jabref
and looking for our final commit (_TODO : put link of this_). As you can see, all tests checking for style pass, which means 
that our patch respects the style of JabRef. You may also go on the  `latexcitation-filemonitor-issue-10585` and take a look at the 
latest code on it to check that our patch is clean. 

Optional (point 5): considered for acceptance (passes all automated checks).
_TODO : link to closed issue / merged pull request, if it has been accepted or remove this section_

## Test results

The final version of the tests ran on our code can be found here : 

_TODO : insert link
to action in JabRef, the last commit or our path_

## UML class diagram and its description

### Key changes/classes affected

_TODO_ Non-optional : Key features affected by the issue are shown in UML class diagrams (for refactorings: include be- fore/after).
Note: you do not have to show classes, fields, or methods that are not relevant, unless they help with the overall understanding. Typically, the diagram would contain 5–10 classes.

Optional (point 1): Architectural overview.

Optional (point 2): relation to design pattern(s).

## Overall experience

What are your main take-aways from this project? What did you learn?

How did you grow as a team, using the Essence standard to evaluate yourself?

Optional (point 6): How would you put your work in context with best software engineering practice?

Optional (point 7): Is there something special you want to mention here?


## P+

As a recapitulation here are the points we've achieved for P+. 

3. Relevant test cases (existing tests and updated/new tests related to the refactored code) are traced to requirements.


4. Your patch is clean in that it (a) removes but does not comment out obsolete code and (b) does not produce extraneous output that is not required for your task (such as debug output) and (c) does not add unnecessary whitespace changes (such as adding or removing empty lines).



6. You can argue critically about the benefits, drawbacks, and limitations of your work carried out, in the context of current software engineering practice, such as the SEMAT kernel (covering alphas other than Team/Way of Working).


5. Patches are accepted by the project, or considered for acceptance. (This requires a link to an accepted commit, or a discussion item.) Note: the patch must be submitted by the assignment deadline, but it may be accepted later.


OR 
8. In the context of Jonas Öberg's lecture last week, where do you put the project that you have chosen in an ecosystem of open-source and closed-source software? Is your project (as it is now) something that has replaced or can replace similar proprietary software? Why (not)?



### TODO 

- For P : point 4 and 7 

- For P+ : point 3 and 8 

- At then end put links that are needed
