# copy to: /etc/init.d/mysql
PATH=/bin:/usr/bin:/sbin:/usr/sbin
NAME="MySQL-8.0"
DAEMON=/usr/sbin/mysqld
PIDFILE=/var/run/mysqld/mysqld.pid

test -f $DAEMON || exit 0

wait_for_started() {
    while /bin/true; do
        sleep 1
        mysqladmin ping >/dev/null 2>&1 && break
    done
}

wait_for_stopped() {
    while /bin/true; do
        sleep 1
        mysqladmin ping >/dev/null 2>&1 || break
    done
}

exec_daemon_start() {
    echo "$NAME is starting..."
    mkdir -p /var/run/mysqld
    chown mysql:mysql /var/run/mysqld
    nohup sudo -u mysql $DAEMON >/dev/null 2>&1 &
    wait_for_started
    echo "$NAME started successfully, Pid=$(cat $PIDFILE)"
}

exec_daemon_stop() {
    echo "$NAME is stopping..."
    kill "$(cat $PIDFILE)"
    wait_for_stopped
    echo "$NAME stopped successfully"
}

echo_is_running() {
    echo "$NAME is running, Pid=$(cat $PIDFILE)"
}

echo_is_not_running() {
    echo "$NAME is not running"
}

echo_failed_to_start() {
    echo "$NAME no need to start"
}

echo_failed_to_stop() {
    echo "$NAME no need to stop"
}

case $1 in
start)
    if [ -f $PIDFILE ]; then
        echo_is_running
        echo_failed_to_start
    else
        exec_daemon_start
    fi
    ;;
stop)
    if [ -f $PIDFILE ]; then
        exec_daemon_stop
    else
        echo_is_not_running
        echo_failed_to_stop
    fi
    ;;
restart)
    if [ -f $PIDFILE ]; then
        exec_daemon_stop
    fi
    exec_daemon_start
    ;;
status)
    if [ -f $PIDFILE ]; then
        echo_is_running
    else
        echo_is_not_running
    fi
    ;;
esac
