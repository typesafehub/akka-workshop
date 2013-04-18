Some common problems and how you can solve them in Akka
=======================================================

This is still work in progress.

* Problem 1

  I want to invoke a remote service with response time SLA. If no response try the backup system

* Problem 2
  
  I want to track the size of my mailbox so that I can take some actions based on the size

* Problem 3

  I want to gracefully stop all the actors so that the pending messages are processed

* Problem 4

  I want to fail-fast when my system is overloaded

* Problem 5

  I want to scale out with router to multiple nodes

* Problem 6

  I want to trace messages that flow through my actor system

* Problem 7

  In my application nodes can come up and go down.I don’t want to queue messages to my worker nodes. Only send work if worker is free 

* Problem 8

  I want to automatically scale up or down based on work load  

* Problem 9
  I have a slow actor. I don’t want to overload it so I need message throttling


