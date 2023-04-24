# SmellDetector

## Usage

```shell
# RefactoringExample
run --args="-os -sf D:/Projects/SmellDetector/smells_output.json -src D:/Projects/SmellDetector/examples/RefactoringExample/src"

# ComplexClass
run --args="-os -sf D:/Projects/SmellDetector/smells_output.json -src D:/Projects/SmellDetector/examples/ComplexClass/src"
```

# Step 0: Implementing smell detectors: ComplexClass and FeatureEnvy

The first step in our project was to implement the two code smell detectors, ComplexClass and FeatureEnvy, to detect
class-level and method-level code defects accordingly.

## ComplexClass

- Location: `src/main/java/cmu/csdetector/smells/detectors/ComplexClass.java`
- Input: `Resource` (`Type`) object
- Output: `Smell` object(s)

ComplexClass is a class-level smell. It is defined as a class that has at least one method with a McCabe Cyclomatic
complexity greater than 10.

## FeatureEnvy

- Location: `src/main/java/cmu/csdetector/smells/detectors/FeatureEnvy.java`
- Input: `Resource` (`Method`) object
- Output: `Smell` object(s)

FeatureEnvy is a method-level smell, where a method is making more external method calls than internal ones.Our
implementation takes a method as the input, finds all method calls from this method, and differentiates the number of
internal and external method calls and makes comparisons.

After collecting all smell classes and methods, we start to apply our Extractor class on smelly instances to find
refactoring opportunities via Extract Method operation.

# Step 1: Building the statement table that maps line numbers to statements

- Location: `src/main/java/cmu/csdetector/extractor/Extractor.java`

From this step, we mainly focus on the refactoring works,
heavily inspired by the paper "Identifying Extract Method Refactoring Opportunities Based on Functional Relevance".

## StatementVisitor

- Location: `src/main/java/cmu/csdetector/extractor/StatementVisitor.java`
- Input: `Resource` (`Method` or `Type`) object
- Output: `SortedMap<Integer, Set<String>> statementsTable` maps line numbers to variable and method call names

StatementVisitor is a visitor class (the subclass of ASTVisitor) that is used to build the statement table.
It overrides many visit() methods to visit different types of AST nodes,
including `MethodInvocation`, `Assignment`, `VariableDeclarationStatement`, etc,
to extract variable names and method calls.

For example, when visiting a `MethodInvocation` node, we can get all identifiers `a`, `b` and `c`, from expressions
where simple names are chained together, like `a.b.c()`. In terms of `Assignment` node, we can extract all operands from
the expression, like `a = b + c`, and get `a`, `b`
and `c`.

Then, by using the compilation unit, we can locate the line number of each variable and method call, and build the
statement table. The statement table is a sorted map from line number to variable and method call, which is similar to
the TABLE 2
example in the paper.

# Step 2: Extract the opportunities for each step

In this step, we continue to implement the paper's algorithm to extract the opportunities with an incremental step size.

## StepIterator

- Location: `src/main/java/cmu/csdetector/extractor/StepIterator.java`
- Input: `SortedMap<Integer, Set<String>> statementsTable` from Step 1
- Output: `Set<List<Integer>> opportunitySet = new HashSet<>()` a set of opportunities, where each opportunity is a
  sorted list of line numbers

StepIterator is an `Iterator` class that is used to extract the opportunities with a given step size.
In our implementation, for step from 1 to `statementsTable.size()`, we iterate through the statement table to find all
opportunities with the given step size by applying the `StepIterator` class. Each opportunity is a list of line numbers
and should have the length greater than 1.

Essentially, the `StepIterator` class is a sliding window with size `step` that iterates through the statement table.

- The `next()` method extracts opportunities based on the given step size and the start index by applying a sliding
  window approach. In the window, we check if the lines shares any common variables or method calls. If so, we put the
  line numbers into the opportunity set. Otherwise, we move the window to the next line (if `hasNext()`) and repeat the
  process.
- The `hasNext()` method returns true if the start index is still within the statement table length.
- The `haveOverlap()` method checks if lines within the window share any common variables or method calls.

# Step 3: Create method declarations for all opportunities

- Location: `src/main/java/cmu/csdetector/extractor/ExtractedMethod.java`
- Input: `List<Integer> opportunity` from Step 2
- Output: `List<ExtractedMethod> extractedMethods` a list of extracted method meta-classes (containing method
  declaration and other information)

This step maps the opportunities to the corresponding method declarations (only the method body).

## createExtractedMethod()

Given the start line number and end line number of an opportunity, we can extract the corresponding method body
by scanning the source code file within this range. It pushes each line as a statement into a method body Block.
Then we use a compilation unit to try to compile it to produce a valid method declaration as the extracted method
(body).

There is an edge case that the opportunity, specified by line numbers, is not a valid method body, which means some
statements are dropped by the compilation unit. In this case, we try to extend end line number to include more lines of
code until the compilation unit can compile it successfully. For example, if an opportunity covers a part of an if
statement, we extend the end line number to try to include the whole if statement block.

## createRefactoredType()

After identifying each extracted method from an opportunity, we need to create a new class with the extracted method
removed. This method excludes the lines given by the opportunity from the original class and re-compiles the remaining
lines of code to a new class using the compilation unit.

However, if the compilation unit produces obvious fewer codes than what the refactored class should have, this
opportunity will be dropped as it is not a valid refactoring opportunity. In other words, while we are creating the
method declaration, we are also filtering out invalid opportunities.

# Step 4: Filter & ranking the opportunities

- Location: `src/main/java/cmu/csdetector/extractor/OpportunityProcessor.java`
- Input: `List<ExtractedMethod> extractedMethods` from Step 3
- Output: `List<ExtractedMethod> extractedMethods` filtered and ranked extracted methods

We continue to implement the paper's algorithm to filter and rank the extracted methods.

## isSimilarSize()

This method implements the formula in page 8:

```text
Difference_in_Size(A,B) = |LOC(A) - LOC(B)| / (Min(LOC(A), LOC(B)) * max_size_difference)
```

We also applied `max_size_difference = 0.2` as the paper suggested to allow 20% larger or smaller.

## isSignificantlyOverlapping()

This method implements the formula in page 8:

```text
Overlap = Overlap(A,B) / Max(LOC(A), LOC(B)) * min_overlap
```

We also applied `min_overlap = 0.1` as the paper suggested to allow 50% overlap.

## calcBenefit()

This method implements the formula in page 9:

```text
BenefitLCOM = LCOM(original) - Max(LCOM(original_after_refactoring), LCOM(opportunity))
```

We also applied `significant_difference_threshold = 0.01`  to decide which one is the optimal when comparing the LCOM
metrics.

# Step 5: Assign the method name, parameters, and return type to each method declaration

- Location: ???
- Input: `List<ExtractedMethod> extractedMethods` from Step 4
- Output: `List<ExtractedMethod> extractedMethods` with the method name, parameters, and return type assigned

> TODO

# Step 6: Finding the Target class for each opportunity

- Location: `src/main/java/cmu/csdetector/extractor/RefactoringEvaluator.java`
- Input: `List<ExtractedMethod> extractedMethods` from Step 5
- Output: ???

Before finding the target class, we need to get all candidate classes in the same package who may be the target class to
move the extracted method to.
In other words, we need to find all classes except the current class that contains the opportunity.

## RefactoringEvaluator

After getting all candidate classes, we apply a nested loop to find the target class for each opportunity.
For each extracted method and a candidate class, we do the following:

1. calculateLCOMBeforeRefactoring(): calculate the LCOM metric of the original class and the candidate class before
   refactoring
2. applyRefactoring(): we make a copy of the directory of the classes and apply the refactoring to the copy.
   Specifically, we add the extracted method to the candidate class and remove the extracted method from the original
   class, and we also copy other classes in the same package to the new directory to help calculate the LCOM since this metric is derived from the classes in the same package.
3. calculateLCOMAfterRefactoring(): calculate the LCOM metric of the original class and the candidate class after
   refactoring
4. cleanUp(): remove the copy directory

After getting the LCOM metrics before and after refactoring, as the lower LCOM metric is better, we can calculate the benefit by `(refactoredSourceClassLCOM + refactoredTargetClassLCOM) - (originalSourceClassLCOM + originalTargetClassLCOM)`
to get the reduction in LCOM metric. This reduction is the benefit of the refactoring, the larger, the better.

For each extracted method, we find the candidate class with the largest benefit and decide the target class for this opportunity.

# Step 7: Generate the output json

Finally, we add the smells, extracted methods, and target classes to the output json file.
