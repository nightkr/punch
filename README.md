Punch
=====

Punch is a rewrite of [Pow][pow] for Linux. It aims for being similarly easy to use, but replacing native Rack support with [Foreman][foreman]. Initially, support for only systemd under Arch is planned, although being able to run under Ubuntu Desktop shouldn't be too far away after then. For now, using [xip.io][xipio] and/or lvh.me (always resolves to 127.0.0.1) is planned instead of messing with resolving .dev domains locally. There's some more typing to access it, but resolving domains (especially messing with these configuration files across distributions in an automated way) is too messy to work with at the moment.


[pow]: http://pow.cx/
[foreman]: https://github.com/ddollar/foreman
[xipio]: http://xip.io/


Limitations
-----------

* No support for HTTP keep-alive
* No built-in DNS server
  * Use lvh.me instead


Punchfile
---------

Some services don't do very well with Foreman for some reason (for exaple [Play][play] applications). For these, you can instead provide a `Punchfile` in the application's directory. This is a simple shell script that Punch then takes care of running and monitoring, instead of starting foreman.


[play]: http://playframework.com/


Installation
------------

### Development build (straight out of git)

1. Install [sbt][sbt] if you don't already have it
2. Create the directory `~/.punch`
3. Start Punch: `$ sbt run` in this folder
4. Set up a Procfile or a Punchfile in your web application's directory

[sbt]: http://scala-sbt.org/

### Arch package

1. Go to the arch folder
2. `$ rm punch-git-*-any.pkg.tar.xz`
3. `$ makepkg -f --holdver`
4. `$ sudo pacman -U punch-git-*-any.pkg.tar.xz`
5. `$ mkdir ~/.punch`
6. `$ sudo systemctl daemon-reload`
7. `$ sudo systemctl enable punch@\`whoami\`.service`
8. `$ sudo systemctl start punch@\`whoami\`.service`


Usage
-----

1. `$ ln -s /path/to/your/application/ ~/.smack/foobar`
2. `$ xdg-open http://foobar.lvh.me:8080/`

The Arch package also includes a reverse proxy on port 80, so you don't need to enter the port number. To disable this, mask `punch-socat.service`.