# SmellDetector

Starter code for the smell detector. This version contains three main packages:
- **Resources**: classes in this package represent and load the source code' elements in a Java project. The two main resources are Method and Type (class, interface and enumerate).
- **AST**: classes in this package handles the _Abstract Syntax Tree_. A sub-package organizes different visitors to extract information from the AST. 
- **Metric**: classes in this package represent different software quality metrics necessary to detect code smells.


See **SmellDetector (V1)** for a simplified class diagram.

NOTE:
- IntelliJ Command to run: run --args="-sf _path_in_your_machine_/smells_output.json -src _path_to_source_code_/src"

