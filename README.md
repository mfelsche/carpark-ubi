# Coding challenge
**Carpark Ubi**

## Domain vocabulary:
EV - electric vehicle.
CP - charging point, an element in an infrastructure that supplies electric energy for the recharging of electric vehicles.

## Problem details:
The task is to implement a simple application to manage the charging points installed at Carpark Ubi.
Carpark Ubi has 10 charging points installed. When a car is connected it consumes either 20 Amperes (fast charging) or 10 Amperes (slow charging). 
Carpark Ubi installation has an overall current input of 100 Amperes so it can support fast charging for a maximum of 5 cars or slow charging for a maximum of 10 cars at one time.
A charge point notifies the application when a car is plugged or unplugged.
The application must distribute the available current of 100 Amperes among the charging points so that when possible all cars use fast charging and when the current is not sufficient some cars are switched to slow charging. 
Cars which were connected earlier have lower priority than those which were connected later.
The application must also provide a report with a current state of each charging point returning a list of charging point, status (free or occupied) and - if occupied – the consumed current.

## Requirements:
1. The solution must be implemented as a Spring Boot application with Java.
2. We need to be able to start it and run tests.
3. BIZ logic needs to be implemented correctly.
4. Interaction with the APP needs to happen through well-defined REST APIs.
4. Include at least one unit test and one integration test.
3. Solution needs to be thread safe.

## Examples:

```
CP1 sends a notification that a car is plugged
Report: 
CP1 OCCUPIED 20A
CP2 AVAILABLE
...
CP10 AVAILABLE
```

```
CP1, CP2, CP3, CP4, CP5 and CP6 send notification that a car is plugged
Report:
CP1 OCCUPIED 10A
CP2 OCCUPIED 10A
CP3 OCCUPIED 20A
CP4 OCCUPIED 20A
CP5 OCCUPIED 20A
CP6 OCCUPIED 20A
CP7 AVAILABLE
...
CP10 AVAILABLE
```

## Deliverables:
Link to the git repository with the implementation and the documentation on how to call the API (Swagger/Postman collection/text description).
Please add any details about your ideas and considerations to this README and add it to the repository.

## Challenge Details

**WARNING: This is my first Spring Boot / Java Enterprise Code, EVER.**

I implemented the challenge modelling the ChargePoints as POJOS persisted in a JPA repository to the H2 embedded database. The overall used ampere are tracked using those ChargePoints in the database with their `status` and `chargeType` fields. When a car is plugged,
the repository is consulted to check if we need to switch chargepoints with cars plugged in earlier to slow charging, in order to provide fast-charge to the new car. When a car is unplugged, the repository is consulted to check if we can switch other chargepoints to fast charging. All this while maintaining the maximum possible ampere of 100A.

Thread safety is achieved by wrapping the operations upon plug and unplug events into `@Transactional`, ensuring a (database?) transaction
is used across several calls to the repository to retrieve and manipulate the current state in one atomic step.

The application is built as REST Api consuming and producing json. Modelling of the endpoints might be a but un-RESTy to some.
The basic idea behind adding an endpoint like  `/chargepoint/<ID>/event` and putting event type and timestamp into url params
was to maybe also record single events in addition to maintaining the computed current state in the ChargePointRepository.
It might be more appropriate to solve it differently but I found it appealing to do so and didnt regret it during implementation.

The Swagger UI can be found at:

http://localhost:8080/swagger-ui.html

It can also be inspected at https://editor.swagger.io using the given `swagger.json` file in this repository.

* There are still tests missing, especially:
  - Verifying that chargepoints with cars plugged in earlier are prioritized when downgrading to slow charging with 10A
  - Verifying that remaining amperes are distributed with priority to chargepoints with cars plugged in later
  - a property test ensuring proper handling and distribution of available ampere to all charging points (not exceeding max ampere) given any order of events
