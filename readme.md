AppMinifier is a set of scripts which help to reduce a size of Java applications.

Usually if a Java app depends on some third-party library, this library is included as-is, even if it contains some classes which are never used by application.
It is especially bad if application depends not on single library, but on whole framework, such as NetBeans platform or Eclipse platform.

AppMinifier will get a list of classes which are actually used by application, and will process jar files to keep only those classes which are used.
To generate a list of classes used by application, run it with a command-line param

-Xlog:class+load=info:classloaded.txt

and while running, use your application in most possible way, activating all possible actions and behaviours. 
Then run AppMinifier and supply it with classloader log. AppMinifier will process all ".jar" files and will create ".jaf" files containing only those classes which are mentioned in classloader log.
Now you can decide which jar files you want to replace with generated jaf files.

For example, VisualVM v. 217 was reduced from approx. 50M to approx. 25M.