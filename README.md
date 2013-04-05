Smack
=====

Smack is a rewrite of [Pow][pow] for Linux. It aims for being similarly easy to use, but replacing native Rack support with [Foreman][foreman]. Initially, support for only systemd under Arch is planned, although being able to run under Ubuntu Desktop shouldn't be too far away after then. For now, using [xip.io][xipio] and/or lvh.me (always resolves to 127.0.0.1) is planned instead of messing with resolving .dev domains locally. There's some more typing to access it, but resolving domains (especially messing with these configuration files across distributions in an automated way) is too messy to work with at the moment.


[pow]: http://pow.cx/
[foreman]: https://github.com/ddollar/foreman
[xipio]: http://xip.io/


Limitations
-----------

* No support for HTTP keep-alive
* No built-in DNS server
  * Use lvh.me instead


Smackfile
---------

Some services don't do very well with Foreman for some reason (for exaple [Play][play] applications). For these, you can instead provide a `Smackfile` in the application's directory. This is a simple shell script that Smack then takes care of running and monitoring, instead of starting foreman.


[play]: http://playframework.com/


Usage
-----

1. Install [sbt][sbt] if you don't already have it
2. Create the directory `~/.smack`
3. Start smack: `$ sbt run` in this folder
4. Set up a Procfile or a Smackfile in your web application's directory
5. `$ ln -s /path/to/your/application/ ~/.smack/foobar`
6. `$ xdg-open http://foobar.lvh.me/`


[sbt]: http://scala-sbt.org/