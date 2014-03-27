WINDOWS

Dependencies:
- wine
- innoextract

Get innoextract on Ubuntu:
- sudo add-apt-repository ppa:arx/release
- sudo apt-get update
- sudo apt-get install innoextract

Create setup:
1. Download "wrapper-delta-pack-*.tar.gz" from http://wrapper.tanukisoftware.com/doc/english/download.jsp
2. Download "isetup-*.exe" from http://www.jrsoftware.org/isdl.php
3. Copy files to "traccar/setup/windows"
4. Change directory to "traccar/setup/windows"
5. Run "./traccar.sh"

LINUX (tested on Ubuntu)

Be sure that `wine` and `innoextract` are installed

Create setup:
1. Download "wrapper-delta-pack-*.tar.gz" (Community one is preferred) from http://wrapper.tanukisoftware.com/doc/english/download.jsp
2. Copy the previous file to "traccar/setup"
3. Download "isetup-*.exe" from http://www.jrsoftware.org/isdl.php
4. Copy the previous file to "traccar/setup"

CONTINUE following the build instructions on http://www.traccar.org/docs/build.jsp

