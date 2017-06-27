
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
