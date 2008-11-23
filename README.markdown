# CLOAK #

Cloak is a simplistic automation tool written in [Clojure][]. It's heavily
inspired by ruby [rake][]. 

Usage
----------

When invoked from a command line without parameters, `cloak` looks for
a file named `CLOAK` in the current directory (this file should define
tasks to be performed) and executes `:default` task.

You can also use the following options:

* `-h` to display help
* `-f cloak_file` to use non default cloak file
* `-d` to describe available task
* `-t` run cloak without executing any actions

You can also specify a series of task names, which will be executed in
sequence (from left to right).

Cloak provides two kinds of tasks: tasks and file tasks. 

File tasks are geared towards *creating* files. Dependencies of a file
task can be other files and tasks. If a dependency is a file,
modification time of source and target files is compared before
executing the task. 

rosado.cloak.actions provides a couple of basic file operations:
`exists?`, `copy` , `move`, `rm`, `sh` (executing a shell
command). Windows users must be careful to use a "cmd ..." as a
parameter to `sh` when running a batch script.

Compiling Clojure sources
-------------------------

Currently AOT (Ahead Of Time) compilation is still an experimental
feature of Clojure (but not for long). It works (look at this
project's CLOAK file) but the following must be true for it to work:

* directory with sources *AND* "classes" directory must be in you
* class path the "classes" directory must exists when the JVM starts up to execute the `compile` function (otherwise strange errors might occur).

What it isn't
-------------

It's not an `ant` or `maven` replacement. No ac


Dependencies
------------

Cloak requires rosado.math.graph and rosado.io, my helper libraries
available as [rosado libs][mylibs] from github. To run the tests
you'll need test-is from [clojure-contrib][contrib].


[rake]:http://rake.rubyforge.org/
[mylibs]:http://github.com/...
[clojure]:http://clojure.org/
[contrib]:http://sourceforge.net/projects/clojure-contrib
