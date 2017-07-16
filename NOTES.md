
##### July 16

Hmmm. Add some exception logging... log to a file... the Future is definitely timing out.

    2017-07-16 22:32:56.820UTC INFO [ip-akka.actor.default-dispatcher-20] t.i.DCounterTreeLeaf - asked: 312027 216.228.72.46 - 1
    2017-07-16 22:32:56.823UTC INFO [ip-akka.actor.default-dispatcher-20] t.i.DCounterTreeLeaf - asked: 312028 67.142.96.53 - 7
    2017-07-16 22:32:56.810UTC INFO [ip-akka.actor.default-dispatcher-20] t.i.DCounterTreeLeaf - asked: 311459 75.163.91.104 - 2
    2017-07-16 22:33:23.200UTC ERROR[ip-akka.actor.default-dispatcher-16] t.b.CountFromFile$ - java.util.concurrent.TimeoutException: Futures timed out after [30 seconds]
    2017-07-16 22:33:23.206UTC INFO [ip-akka.actor.default-dispatcher-10] t.Terminator - Actor[akka://ip/user/emitter#-2055043580] has terminated, shutting down system

Meanwhile there are dead letters:

    2017-07-16 22:32:56.295UTC INFO [ip-akka.actor.default-dispatcher-2] a.a.DeadLetterActorRef - Message [traffic.CountsTree] from Actor[akka://ip/user/counter/counter-103/counter-103-19/counter-103-19-173#1933246622] to Actor[akka://ip/deadLetters] was not delivered. [8] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
    2017-07-16 22:32:56.295UTC INFO [ip-akka.actor.default-dispatcher-2] a.a.DeadLetterActorRef - Message [traffic.CountsTree] from Actor[akka://ip/user/counter/counter-104/counter-104-1/counter-104-1-169#1017696476] to Actor[akka://ip/deadLetters] was not delivered. [9] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
    2017-07-16 22:32:56.295UTC INFO [ip-akka.actor.default-dispatcher-2] a.a.DeadLetterActorRef - Message [traffic.CountsTree] from Actor[akka://ip/user/counter/counter-100/counter-100-0/counter-100-0-178#2134755659] to Actor[akka://ip/deadLetters] was not delivered. [10] dead letters encountered, no more dead letters will be logged. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

I didn't notice the last message, before.  No wonder there are always 10, that's the limit!

Could it be that some of the actors are gone, and we're waiting for them to response, hence the timeout?

Let's up the number of dead letters.  10K? Yikes, hit the limit.  100K?  13140 recorded.  That's a lot. 

...

Add a listener via https://stackoverflow.com/a/23911129

2017-07-16 22:54:44.945UTC ERROR[ip-akka.actor.default-dispatcher-25] t.DeadLetterListener - akka://ip/user/counter/counter-96/counter-96-81 failed to akka://ip/deadLetters
2017-07-16 22:54:44.945UTC ERROR[ip-akka.actor.default-dispatcher-25] t.DeadLetterListener - akka://ip/user/counter/counter-69/counter-69-120 failed to akka://ip/deadLetters
2017-07-16 22:54:44.945UTC ERROR[ip-akka.actor.default-dispatcher-25] t.DeadLetterListener - akka://ip/user/counter/counter-73/counter-73-115 failed to akka://ip/deadLetters

WTF?  failed to deliver TO the dead letter queue???




##### June 30 2017 - Pro-D!

Thinking about it the past few days...

* definitely want to NOT have any actor keeping more than one count, that would cause a lot of congestion
* there are various ways I've thought of to manage obsolete counts
  * send a message back to parent saying you're zero, parent can put you in a different map
  * if you've been in that map for awhile, delete you
  * or... stuff like that... there always seems to be an edge race condition
  * but it's possible
* but it's not necessary!!  For now, just don't include 0 counts in the output, ignore them
  * in the future if it seems we really need to free them up, worry about them then
* while we're doing the tree count, also count the number of actors, and the number of zero leaves
  * so we know number of actors, and number of active addresses
  
...

Okay, counts are only in leaves, now. Did a bunch of renaming. I get a count of the actors.

So what about real data?  Tried pulling from Sumo, but turns out it exports at most 100K lines, bah!

What about the ELB logs in S3?  Aha!  Download a day's worth, takes awhile.  Eventually figure out the pipe 

     cat t | cut -b1-100 | cut -d' ' -f1,3 | sed -e 's/:[[:digit:]]*$//g'
     
I tried reading and... it timed out processing 5M... raise the timeout to five minutes, still not good.  Hmm.

Take it down to 1M, fast. 2M a dozen messages to dead letter, and hangs.  Finally get it to 1.5M.  Not 1.6M. Weird.
Though that's default memory settings.  Regardless, pulling out the counts takes only milliseconds for 1.5M records, so 
I'm on to *something*

    17:21:24.691 [ip-akka.actor.default-dispatcher-4] INFO traffic.by_ip_address.CountFromFile$ - reading...
    17:21:42.942 [ip-akka.actor.default-dispatcher-2] INFO traffic.by_ip_address.CountFromFile$ - extracting counts
    17:21:43.859 [ip-akka.actor.default-dispatcher-6] INFO traffic.by_ip_address.CountFromFile$ - 1500000 249041 0

Well, okay, almost a second.  There are ways to optimize that (cache the length of the list, for instance).

Interesting that when it hangs up, there are always exactly 10 undelivered messages.



##### June 27 2017

So I've got a problem, I realized last night: when a node goes to zero and stops, it is still referenced in the parent's map.
So subsequent counts will fail on a dead reference.  

So (I use 'so' too much) the child should send a message to the parent saying it's zero and should be closed down. Easy enough.
The parent is responsible for starting, it should be responsible for stopping.

But... what if an increment comes in between the reset and the 'shut me down'?  That increment will be lost.

Hmmm.

Poke around a bit, but don't find anything. And it still feels wrong. 

The problem is the responsibility for deciding when to start or stop is split across the two actors. Perhaps the parent should make all the choices? But that means that parent needs to know the count, so it know when to shut a child down. Which would fix the race condition.

Now, we can't just ask the child each time what its count is, that's asynchronous. 

It starts feeling heavy though, the parent has a link to a child, *and* has its counts... 
I guess you just think of it parenting more parents. Hmmmm.

I like this puzzling things out. Though perhaps wouldn't appreciate doing it under pressure.

...

BUT! If we do that, then each parent has to do all the work of counting, and of forgeting... 
and if each stage keeps its own aggregate count, then... the top node is working for EVERY SINGLE VALUE.

Ug. That doesn't scale at all.

Maybe... maybe only the *leaves* should be counting, the rest should aggregate as needed? 
(Do we really need to alarm on the interim values?) 

Then the work of filtering out old data only happens at the leaves (good).

And then... maybe we don't *need* to immeditately delete zero leaves. Just ignore them? 
And periodically garbage-collect them? Recursively?  Hmm, could still get in a race condition
 for higher-level nodes.
 
 


##### June 26 2017

Thinking about how to start taking time into account, i.e say we just want numbers for the last hour, or day?

Map? Hmmm, google... turns out Guava has something interesting: https://google.github.io/guava/releases/17.0/api/docs/com/google/common/cache/CacheBuilder.html

Cool. But... overkill? And do I really need a map? Really... just a list... of timestamp.

Heck, not even, just longs!  Much easier to compare.

So do some coding, not hard to break out a "counting strategy" trait to manage that. Well, okay, first just changed it, then realized I'd want to be able to switch it out, to compare.

So made a strategy, got that going. `takeWhile` will hack off the tail. Okay.

But... first I did when incrementing.  Ooops, it will never time out.

So make it chop the tail on read.

Okay, that worked, but... now got a lot of zero counts. Don't want zero counts, we want the actor to go away when the count clears (it's no longer relevant).

Well... what, do the read of the count, and kill the actor then, still returning 0?  That doesn't make sense.

So... better to just do it ad hoc. Send a command to the actor, then there are no contention issues. And it reduces the counting actors' responsibilities.

And that *seems* to work... geez, I really need to start unit-testing.

But... it's also killing the top node, oops! Guess it needs to extend not the same thing.
 
Anyway.

(BTW, did this all during first day of CTAConf)


##### June 18 2017

Much simpler and cleaner to pass the "notifier" in via a message, rather than using constructors.

But... doesn't work.  Or... oh. Simply sending the "set" message after the counter is initially built only adds it at the top level.  Not too children.

So will need to endure the current notifier is added in when the children are created. Which means... sending a message to each after it's created. Or putting the constructor arg back in?  Hmm.

...

Okay, not SO hard... just needed to connect the `ChildFactory` and `Counting` traits via a callback. Not ideal, I'd like to ultimately separate the *tree-ness* but... good for now.

Also, now it seems so long as I call `system.terminate` at the end of the program, the app stops. Hmmm.


##### June 16, 2017

Seems it's easy to forget what I've done if I put this down for awhile.

When I stopped last, I was having trouble with the app not stopping.  Turns out all the actors were still running, so duh.  But the `Terminator` doesn't seem to work, I had to shut down the actor system myself (this is just in `CountRandom`).

I tried to get logging working... frustratingly the Akka docs didn't seem to work, and I got complaints that the SLF jar wasn't there.  Eventually some Googling made it work. You need an Akka SLF4J lib, which is not a surprise, but's not in the Akka docs.

Okay, so it seems to be counting correctly, and I can build a tree of output, but... that's not what will be interesting.  I want the actors to tell me when there's a problem.

But how do that?  Every actor needs some way to communicate there's a problem.... ug, that's a lot of constructors to modify, and it seems messy. But it initially works.

But in this revision there's a lot of duplication, it feels like, and tweaking the numbers is awkward. Think I'll need to wrap the actor up, and not have it sent in directly.
