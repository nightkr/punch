Smack
=====

Smack is a rewrite of [Pow][pow] for Linux. It aims for being similarly easy to use, but replacing native Rack support with [Foreman][foreman]. Initially, support for only systemd under Arch is planned, although being able to run under Ubuntu Desktop shouldn't be too far away after then. For now, using [xip.io][xipio] and/or lvh.me (always resolves to 127.0.0.1) is planned instead of messing with resolving .dev domains locally. There's some more typing to access it, but resolving domains (especially messing with these configuration files across distributions in an automated way) is too messy to work with at the moment.


[Pow]: http://pow.cx/
[Foreman]: https://github.com/ddollar/foreman
[xipio]: http://xip.io/


Limitations
-----------

* No support for HTTP keep-alive
* No built-in DNS server
** Use lvh.me instead
