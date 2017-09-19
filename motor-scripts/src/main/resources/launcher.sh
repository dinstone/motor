#!/bin/sh

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set LAUNCHER_HOME if not already set
[ -z "$LAUNCHER_HOME" ] && LAUNCHER_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`


if [ ! -r "$LAUNCHER_HOME/bin/bootstrap.jar" ]; then
	echo "Cannot find $LAUNCHER_HOME/bin/bootstrap.jar"
	echo "The file is absent or does not have execute permission"
	echo "This file is needed to run this program"
	exit 1
fi

CLASSPATH=$LAUNCHER_HOME/bin/bootstrap.jar
MAINCLASS=com.dinstone.motor.launcher.Launcher

LAUNCHER_TMPDIR=$LAUNCHER_HOME/temp
LAUNCHER_CONFIG=$LAUNCHER_HOME/bin/launcher.properties
if [ -r "$LAUNCHER_CONFIG" ]; then
	LAUNCHER_OPTS=-Dlauncher.home=$LAUNCHER_HOME -Dlauncher.config=$LAUNCHER_CONFIG
else
	LAUNCHER_OPTS=-Dlauncher.home=$LAUNCHER_HOME
fi
LAUNCHER_OUT=$LAUNCHER_HOME/logs/launcher.out

RUN_JAVA=java
GC_LOG=$LAUNCHER_HOME/logs/gc.log
JAVA_OPTS="-server -Xss{{Xss:256k}} -Xms{{Xms:1g}} -Xmx{{Xmx:1g}} -Xmn{{Xmn:256m}} -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
JAVA_GC="-verbose:gc -Xloggc:$GC_LOG -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi
# Bugzilla 37848: only output this if we have a TTY
if [ $have_tty -eq 1 ]; then
  echo "Using LAUNCHER_HOME: $LAUNCHER_HOME"
  echo "Using LAUNCHER_OPTS: $LAUNCHER_OPTS"
  echo "Using JAVA_OPTS    : $JAVA_OPTS"
  if [ ! -z "$LAUNCHER_PID" ]; then
    echo "Using LAUNCHER_PID: $LAUNCHER_PID"
  fi
fi

# Execute 
if [ "$1" = "start" ] ; then

  if [ ! -z "$LAUNCHER_PID" ]; then
    if [ -f "$LAUNCHER_PID" ]; then
      if [ -s "$LAUNCHER_PID" ]; then
        echo "Existing PID file found during start."
        if [ -r "$LAUNCHER_PID" ]; then
          PID=`cat "$LAUNCHER_PID"`
          ps -p $PID >/dev/null 2>&1
          if [ $? -eq 0 ] ; then
            echo "Launcher appears to still be running with PID $PID. Start aborted."
            echo "If the following process is not a Launcher process, remove the PID file and try again:"
            ps -f -p $PID
            exit 1
          else
            echo "Removing/clearing stale PID file."
            rm -f "$LAUNCHER_PID" >/dev/null 2>&1
            if [ $? != 0 ]; then
              if [ -w "$LAUNCHER_PID" ]; then
                cat /dev/null > "$LAUNCHER_PID"
              else
                echo "Unable to remove or clear stale PID file. Start aborted."
                exit 1
              fi
            fi
          fi
        else
          echo "Unable to read PID file. Start aborted."
          exit 1
        fi
      else
        rm -f "$LAUNCHER_PID" >/dev/null 2>&1
        if [ $? != 0 ]; then
          if [ ! -w "$LAUNCHER_PID" ]; then
            echo "Unable to remove or write to empty PID file. Start aborted."
            exit 1
          fi
        fi
      fi
    fi
  fi

  shift

  eval "\"$RUN_JAVA\"" $JAVA_OPTS $JAVA_GC $LAUNCHER_OPTS -Djava.io.tmpdir="\"$LAUNCHER_TMPDIR\"" \
	  -classpath $CLASSPATH $MAINCLASS "$@" start >> "$LAUNCHER_OUT" 2>&1 "&"

  if [ ! -z "$LAUNCHER_PID" ]; then
    echo $! > "$LAUNCHER_PID"
  fi

  echo "Launcher started."

elif [ "$1" = "stop" ] ; then

  shift

  SLEEP=5
  if [ ! -z "$1" ]; then
    echo $1 | grep "[^0-9]" >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      SLEEP=$1
      shift
    fi
  fi

  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

  if [ ! -z "$LAUNCHER_PID" ]; then
    if [ -f "$LAUNCHER_PID" ]; then
      if [ -s "$LAUNCHER_PID" ]; then
        kill -0 `cat "$LAUNCHER_PID"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          echo "PID file found but no matching process was found. Stop aborted."
          exit 1
        fi
      else
        echo "PID file is empty and has been ignored."
      fi
    else
      echo "\$LAUNCHER_PID was set but the specified file does not exist. Is Launcher running? Stop aborted."
      exit 1
    fi
  fi

  eval "\"$RUN_JAVA\"" $JAVA_OPTS $JAVA_GC $LAUNCHER_OPTS -Djava.io.tmpdir="\"$LAUNCHER_TMPDIR\"" -classpath $CLASSPATH $MAINCLASS "$@" stop

  # stop failed. Shutdown port disabled? Try a normal kill.
  if [ $? != 0 ]; then
    if [ ! -z "$LAUNCHER_PID" ]; then
      echo "The stop command failed. Attempting to signal the process to stop through OS signal."
      kill -15 `cat "$LAUNCHER_PID"` >/dev/null 2>&1
    fi
  fi

  if [ ! -z "$LAUNCHER_PID" ]; then
    if [ -f "$LAUNCHER_PID" ]; then
      while [ $SLEEP -ge 0 ]; do
        kill -0 `cat "$LAUNCHER_PID"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$LAUNCHER_PID" >/dev/null 2>&1
          if [ $? != 0 ]; then
            if [ -w "$LAUNCHER_PID" ]; then
              cat /dev/null > "$LAUNCHER_PID"
              # If Launcher has stopped don't try and force a stop with an empty PID file
              FORCE=0
            else
              echo "The PID file could not be removed or cleared."
            fi
          fi
          echo "Launcher stopped."
          break
        fi
        if [ $SLEEP -gt 0 ]; then
          sleep 1
        fi
        if [ $SLEEP -eq 0 ]; then
          if [ $FORCE -eq 0 ]; then
            echo "Launcher did not stop in time. PID file was not removed. To aid diagnostics a thread dump has been written to standard out."
            kill -3 `cat "$LAUNCHER_PID"`
          fi
        fi
        SLEEP=`expr $SLEEP - 1 `
      done
    fi
  fi

  KILL_SLEEP_INTERVAL=5
  if [ $FORCE -eq 1 ]; then
    if [ -z "$LAUNCHER_PID" ]; then
      echo "Kill failed: \$LAUNCHER_PID not set"
    else
      if [ -f "$LAUNCHER_PID" ]; then
        PID=`cat "$LAUNCHER_PID"`
        echo "Killing Launcher with the PID: $PID"
        kill -9 $PID
        while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
            kill -0 `cat "$LAUNCHER_PID"` >/dev/null 2>&1
            if [ $? -gt 0 ]; then
                rm -f "$LAUNCHER_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ -w "$LAUNCHER_PID" ]; then
                        cat /dev/null > "$LAUNCHER_PID"
                    else
                        echo "The PID file could not be removed."
                    fi
                fi
                # Set this to zero else a warning will be issued about the process still running
                KILL_SLEEP_INTERVAL=0
                echo "The Launcher process has been killed."
                break
            fi
            if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                sleep 1
            fi
            KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
        done
        if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
            echo "Launcher has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
        fi
      fi
    fi
  fi

elif [ "$1" = "info" ] ; then

    "$RUN_JAVA" -classpath $CLASSPATH $MAINCLASS info

else

  echo "Usage: launcher.sh ( commands ... )"
  echo "commands:"
  echo "  start             Start launcher in a separate window"
  echo "  stop              Stop launcher, waiting up to 5 seconds for the process to end"
  echo "  stop n            Stop launcher, waiting up to n seconds for the process to end"
  echo "  stop -force       Stop launcher, wait up to 5 seconds and then use kill -KILL if still running"
  echo "  stop n -force     Stop launcher, wait up to n seconds and then use kill -KILL if still running"
  echo "  info              Show system info"
  echo "Note: Waiting for the process to end and use of the -force option require that \$LAUNCHER_PID is defined"
  exit 1

fi

