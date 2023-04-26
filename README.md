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

## Output Example

### RefactoringExample

```json
{
  "smells": [
    {
      "name": "FeatureEnvy",
      "reason": "EXTERNAL_METHOD_CALLS to Movie (3) > INTERNAL_CALLS (0)",
      "startingLine": 13,
      "endingLine": 58
    },
    {
      "name": "FeatureEnvy",
      "reason": "EXTERNAL_METHOD_CALLS to Rental (9) > INTERNAL_CALLS (0)",
      "startingLine": 13,
      "endingLine": 58
    },
    {
      "name": "FeatureEnvy",
      "reason": "EXTERNAL_METHOD_CALLS to Tape (3) > INTERNAL_CALLS (0)",
      "startingLine": 13,
      "endingLine": 58
    }
  ]
}
```

### ComplexClass

```json
{
  "smells": [
    {
      "name": "ComplexClass",
      "reason": "CC = 12.0",
      "startingLine": 3,
      "endingLine": 53
    }
  ]
}
```

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
the expression, like `a = b + c`, and get `a`, `b` and `c`.

Then, by using the compilation unit, we can locate the line number of each variable and method call, and build the
statement table. The statement table is a sorted map from line number to variable and method call, which is similar to
the TABLE 2 example in the paper.

## Output Example

### RefactoringExample

```text
===== Statement Table =====
Line Number | Statements
         14 | totalAmount
         15 | frequentRenterPoints
         16 | this.rentals, iterator, this, rentals
         17 | result, name
         19 | hasNext, rentals, rentals.hasNext
         20 | thisAmount
         21 | next, rentals.next, rentals, each
         24 | priceCode, each.getTape, getMovie, getTape, each
         25 | Movie, REGULAR, Movie.REGULAR
         26 | thisAmount
         27 | daysRented, each, each.daysRented
         28 | daysRented, thisAmount, each, each.daysRented
         30 | NEW_RELEASE, Movie, Movie.NEW_RELEASE
         31 | daysRented, thisAmount, each, each.daysRented
         33 | Movie, Movie.CHILDREN, CHILDREN
         34 | thisAmount
         35 | daysRented, each, each.daysRented
         36 | daysRented, thisAmount, each, each.daysRented
         40 | totalAmount, thisAmount
         46 | NEW_RELEASE, Movie, priceCode, daysRented, each.getTape, getMovie, Movie.NEW_RELEASE, getTape, each, each.daysRented
         49 | result, getName, each.getTape, getMovie, getTape, each
         53 | result, totalAmount
         54 | result, frequentRenterPoints
         56 | result
```

### ComplexClass

```text
===== Statement Table =====
Line Number | Statements
          6 | rcs, length, manifests, rcs.length
          8 | rcs, length, i, rcs.length
          9 | rec
         10 | rcs, i
         11 | rec, rcs, i, grabRes
         12 | rcs, i
         13 | rec, rcs, i, grabNonFileSetRes
         15 | rec.length, rec, length, j
         16 | rec, getName, name, replace, j
         17 | rcs, i
         19 | rcs, i, afs
         20 | getFullPath, getProj, equals, afs.getFullPath, afs
         21 | getFullPath, getProj, name, afs.getFullPath, afs
         22 | getFullPath, getProj, equals, afs.getPref, getPref, afs.getFullPath, afs
         23 | pr, getProj, afs.getPref, getPref, afs
         24 | pr, endsWith, pr.endsWith
         25 | pr
         27 | pr, name
         30 | name, equalsIgnoreCase, MANIFEST_NAME, name.equalsIgnoreCase
         31 | rec, manifests, i, j
         35 | manifests, i
         36 | manifests, i
         39 | manifests
```

# Step 2: Extract the opportunities for each step

In this step, we continue to implement the paper's algorithm to extract the opportunities with an incremental step size.

## StepIterator

- Location: `src/main/java/cmu/csdetector/extractor/StepIterator.java`
- Input: `SortedMap<Integer, Set<String>> statementsTable` from Step 1
- Output: `Set<List<Integer>> opportunitySet = new HashSet<>()` a set of opportunities, where each opportunity is a
  sorted list of line numbers

StepIterator is an `Iterator` class that is used to extract the opportunities with a given step size.
In our implementation, for step from 1 to the size of the method that the opportunity is from, we iterate through the statement table to find all
opportunities with the given step size by applying the `StepIterator` class. Each opportunity is a list of line numbers
and should have the length greater than 1.

Bascially, we constructed a matrix. matrix[variable_idx][statement_number] = 1 when the variable appears in the statement.
Then, we followed the steps provided by the paper. From that matrix, we constructed and merged interval for each step. After that, we removed the duplicates.
From there on, we got the list of opportunities, which were shown below. 

### RefactoringExample

```text
===== Opportunities =====
Start Line | End Line | Variables
        27 |       28 | daysRented, each, each.daysRented, daysRented, thisAmount, each, each.daysRented
        35 |       36 | daysRented, each, each.daysRented, daysRented, thisAmount, each, each.daysRented
        53 |       54 | result, totalAmount, result, frequentRenterPoints
        19 |       21 | hasNext, rentals, rentals.hasNext, next, rentals.next, rentals, each
        26 |       28 | thisAmount, daysRented, thisAmount, each, each.daysRented
        34 |       36 | thisAmount, daysRented, thisAmount, each, each.daysRented
        46 |       49 | NEW_RELEASE, Movie, priceCode, daysRented, each.getTape, getMovie, Movie.NEW_RELEASE, getTape, each, each.daysRented, result, getName, each.getTape, getMovie, getTape, each
        16 |       36 | this.rentals, iterator, this, rentals, daysRented, thisAmount, each, each.daysRented
        49 |       53 | result, getName, each.getTape, getMovie, getTape, each, result, totalAmount
        20 |       40 | thisAmount, totalAmount, thisAmount
        16 |       40 | this.rentals, iterator, this, rentals, totalAmount, thisAmount
        20 |       46 | thisAmount, NEW_RELEASE, Movie, priceCode, daysRented, each.getTape, getMovie, Movie.NEW_RELEASE, getTape, each, each.daysRented
```

### ComplexClass

```text
===== Opportunities =====
Start Line | End Line | Variables
         6 |       21 | rcs, length, manifests, rcs.length, getFullPath, getProj, name, afs.getFullPath, afs
        15 |       16 | rec.length, rec, length, j, rec, getName, name, replace, j
        35 |       36 | manifests, i, manifests, i
        10 |       13 | rcs, i, rec, rcs, i, grabNonFileSetRes
         6 |       27 | rcs, length, manifests, rcs.length, pr, name
        31 |       36 | rec, manifests, i, j, manifests, i
        19 |       25 | rcs, i, afs, pr
         6 |       30 | rcs, length, manifests, rcs.length, name, equalsIgnoreCase, MANIFEST_NAME, name.equalsIgnoreCase
         6 |       16 | rcs, length, manifests, rcs.length, rec, getName, name, replace, j
        17 |       27 | rcs, i, pr, name
        19 |       30 | rcs, i, afs, name, equalsIgnoreCase, MANIFEST_NAME, name.equalsIgnoreCase
         6 |       19 | rcs, length, manifests, rcs.length, rcs, i, afs
```

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

- Location: `src/main/java/cmu/csdetector/extractor/Extractor.java`
- Input: `List<ExtractedMethod> extractedMethods` from Step 4
- Output: `List<ExtractedMethod> extractedMethods` with the method name, parameters, and return type assigned

For inferring the name from method body, we used an OpenAI model named `text-davinci-003` and prompted the model to respond with a resaonable method name. 


For assigning the parameters for the method, we used `MethodVariableCollector` to collect all the variables from the larger method that the extracted portion belongs to.
After we get this list of variables, we iterated through this list and filtered the variables that were in the extracted method (which were the ones that had the condition `startlinenumber > em.startLineNumber` and `endlinenumber < em.endLineNumber`)
Then for all these variables in `em`, if they were not declared in `em`, they should be added in the parameters list.


For assigning the return list and return type to the extracted method, we came up with the following logic: For each variable in the extracted method, if it was reassigned somehow and was used after the extracted method, then it should be returned.
Thus, we applied `MethodAssignmentCollector` to collect all the `SimpleName` that was on the left side of the assignment. 
If this `SimpleName` was used after the extracted method (eg: its `startlinenumber > em.endLineNumber`), we added it to the return variable list. 
We also applied the helper function `constructTypeFromString()` to obtain the type of the returned variable and set it as the return type of the extracted method. 


# Step 6: Finding the Target class for each opportunity

- Location: `src/main/java/cmu/csdetector/extractor/RefactoringEvaluator.java`
- Input: `List<ExtractedMethod> extractedMethods` from Step 5
- Output: `Map<MethodDeclaration, Map<Type, Double>> extractionImprovements` a map of extracted method to a map of
  candidate class to the LCOM improvement

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

## Output Example

### RefactoringExample

```text
===== Extracted Method =====
public void calculate(Iterator<Rental> rentals,double totalAmount,int frequentRenterPoints){
  double thisAmount=0;
  Rental each=rentals.next();
switch (each.getTape().getMovie().priceCode()) {
case Movie.REGULAR:
    thisAmount+=2;
  if (each.daysRented() > 2)   thisAmount+=(each.daysRented() - 2) * 1.5;
break;
case Movie.NEW_RELEASE:
thisAmount+=each.daysRented() * 3;
break;
case Movie.CHILDREN:
thisAmount+=1.5;
if (each.daysRented() > 3) thisAmount+=(each.daysRented() - 3) * 1.5;
break;
}
totalAmount+=thisAmount;
frequentRenterPoints++;
if ((each.getTape().getMovie().priceCode() == Movie.NEW_RELEASE) && each.daysRented() > 1) frequentRenterPoints++;
}

Skip negative refactoring: Tape
Skip negative refactoring: Rental

* Target class: Movie
    Source class LCOM before refactoring: 0.625
    Target class LCOM before refactoring: 0.95
    Source class LCOM after refactoring: 0.625
    Target class LCOM after refactoring: 0.875
        LCOM reduction (improvement): -0.07499999999999996

Skip negative refactoring: Registrar

*** Best Target Class: Movie
```

### ComplexClass

```text
===== Extracted Method =====

public void getFullPath(Resource[] rcs,int i,String name,int j){
  ArchiveFileSet afs=(ArchiveFileSet)rcs[i];
  if (!"".equals(afs.getFullPath(getProj()))) {
    name=afs.getFullPath(getProj());
  }
 else   if (!"".equals(afs.getPref(getProj()))) {
    String pr=afs.getPref(getProj());
    if (!pr.endsWith("/") && !pr.endsWith("\\")) {
      pr+="/";
    }
    name=pr + name;
  }
}

* Source class: ComplexClass
    Source class CC before refactoring: 
    {   paper.example.ComplexClass.grabNonFileSetRes=1.0, 
        paper.example.ComplexClass.grabRes=1.0,
        paper.example.ComplexClass.getProj=1.0,
        paper.example.ComplexClass.gradManifests=12.0   }

    Source class CC after refactoring:
    {   paper.example.ComplexClass.getFullPath=5.0, 
        paper.example.ComplexClass.grabNonFileSetRes=1.0, 
        paper.example.ComplexClass.grabRes=1.0,
        paper.example.ComplexClass.getProj=1.0,
        paper.example.ComplexClass.gradManifests=6.0    }
```

# Step 7: Generate the output json

Finally, we add the smells, extracted methods, and target classes to the output json file.
