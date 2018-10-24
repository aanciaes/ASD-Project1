# ASD-Project1

Distributed Systems and Algorithms Project Phase 1

## 1 Introduction and Goals

The goal of this phase of the Project is to implement a scalable (global) membership service that provides to each
process in a (potentially large-scale distributed system with around 10.000 processes) a global view of all other processes
in the system currently. This membership service should be built on top of a partial view membership service
(potentially building an unstructured overlay network, although you are free to select a different overlay topology) and
some information dissemination system.
Informally, the membership service (the top level one) should ensure that eventually, all processes that are correct and
part of the system should be reported as part of the membership by every correct process, and that eventually, all failed
processes are eventually not reported as part of the membership by every correct process.

A small application should be built on top of the membership service that enables a human user to interact with the
system and extract the following information on demand and at any time: i) the view of the local node regarding
the composition of the global membership; ii) the partial view composition in the lower layer; iii) the number of
messages sent and received by the information dissemination system (for the last one, students can provide either an
aggregated value, or a value for each type of message disseminated in their solution).
The following Figure provides an abstract depiction of the algorithms (and application) that each process should have:
```
                    ------------------------------------------------------------------
                   |                          Test Application                       |
                    ------------------------------------------------------------------
                   |                         Global Membership                       | 
                    ------------------------------------------------------------------
                   |             Information Dissemination (Based on Gossip)         |
                    ------------------------------------------------------------------
                   |              Partial View Membership (Overlay Network)          |
                    ------------------------------------------------------------------
 ```


## 2 Implementation Suggestions

You can implement the project in Java, Scala, or Akka (with Scala). If you want to implement this in a different
language you should discuss that option with the Course Professor. Using Akka, you probably want to have each
process having a set of Actors, where each actor represents one of the layers depicted above. Notice that, you cannot
make use of any mechanism provided by the runtime to get knowledge about the system membership.
In relation to each of the layers, the materialization is the responsibility of students. However, you can consider the
following suggestions:

* *Partial View Membership: You can get ideas from Cyclon, Scamp, or HyParView. While you could implement
one of these algorithms you can also make modifications to them, or combine ideas from multiple algorithms.*
* *Information Dissemination System: You should think about using a gossip-based alternative to avoid overloading
a process. There are multiple alternatives here that can be employed, however remember that you should strive to
be fast in propagating information for the layer above, while using minimal network resources.*
* *Global Membership: You will have to detect failures in some way, strive to avoid false positives (the algorithm
incorrectly assumes a process to be failed when it is not). A form of heartbeat can be used here, but evidently it
should not resort to a point-to-point heartbeat from all processes to all processes (that would defeat the purpose of
achieving scalability).*

### 2.1 Evaluation Rules & Criteria

Projects should be conducted by groups of at most 3 students. Single student groups are highly not advised.
The project includes both the delivery of the code and a written report that must contain clear and readable pseudo-code
for each of the implemented layers alongside a description of the intuition of the protocol. A correctness argument
for each layer will be positively considered during grading. The written report should provide information about any
experimental work conducted by the students to evaluate their solution in practice (including results).
The project will be evaluated by the correctness of the implemented solutions, its efficiency, and the quality of the
implementation. With a distribution of 50%, 25%, and 25% respectively for each of the layers.
The quality and clearness of the report of the project will impact the final grade in 10%, however the students should
notice that a poor report might have a significant impact on the evaluation of the correctness of the used solutions
(which weight 50% of the evaluation for each component of the solution).
Each component of the project will have a maximum grade of (out of 20):

* *Partial Membership: 6/20.*
* *Information Dissemination: 6/20.*
* *Global Membership: 6/20.*
* *Test Application: 1/20.*
* *Written Report: 2/20*

(Yes, the sum of this is not 20)

### 2.2 Delivery Rules

Delivery of all components of the project is due on 21 October 2017.
The methods for delivery will be provided soon.                                               
