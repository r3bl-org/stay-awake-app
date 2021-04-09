<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [stay-awake-app](#stay-awake-app)
- [Future of this app is in question](#future-of-this-app-is-in-question)
- [Change master to main (2020-06-27)](#change-master-to-main-2020-06-27)
  - [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# stay-awake-app

This app is like Caffeine for Mac. It keeps your device awake as long as it's connected to power. It
also uses quick settings tiles from Android N as its main interface. It also works on Android O
(with the new background behaviors).

# Future of this app is in question

Android is deprecating most of the APIs that this app relies on and more than likely this app will
be deprecated soon. It heavily relies on background operation and the ability to acquire a wake
lock, both of which are things that Android does not want apps to do anymore.

Here's [an article](https://medium.com/nala-money/the-bifurcation-of-android-6fa1cced074d) on how
the Android OS is implemented by various device manufacturers & carriers affects what can be done by
app developers using the APIs that are implemented differently on each device.

# Change master to main (2020-06-27)

The
[Internet Engineering Task Force (IETF) points out](https://tools.ietf.org/id/draft-knodel-terminology-00.html#rfc.section.1.1.1)
that "Master-slave is an oppressive metaphor that will and should never become fully detached from
history" as well as "In addition to being inappropriate and arcane, the
[master-slave metaphor](https://github.com/bitkeeper-scm/bitkeeper/blob/master/doc/HOWTO.ask?WT.mc_id=-blog-scottha#L231-L232)
is both technically and historically inaccurate." There's lots of more accurate options depending on
context and it costs me nothing to change my vocabulary, especially if it is one less little speed
bump to getting a new person excited about tech.

You might say, "I'm all for not using master in master-slave technical relationships, but this is
clearly an instance of master-copy, not master-slave"
[but that may not be the case](https://mail.gnome.org/archives/desktop-devel-list/2019-May/msg00066.html).
Turns out the original usage of master in Git very likely came from another version control system
(BitKeeper) that explicitly had a notion of slave branches.

- https://dev.to/lukeocodes/change-git-s-default-branch-from-master-19le
- https://www.hanselman.com/blog/EasilyRenameYourGitDefaultBranchFromMasterToMain.aspx

[#blacklivesmatter](https://blacklivesmatter.com/)

## License

Copyright 2020 R3BL, LLC.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
See the NOTICE file distributed with this work for additional information regarding copyright
ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.
