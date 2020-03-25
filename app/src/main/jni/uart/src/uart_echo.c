//
// Created by joseth on 20-3-25.
//
#define LOG_TAG "uart_echo"
#define CONFIG_DEBUG

#include <uart.h>
#include <signal.h>
#include <pthread.h>
#include <mc_log.h>
#include <unistd.h>
#include <getopt.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>

#define UART_PATH   "/dev/ttyS0"
#define UART_BAUDRATE   115200

static int running = 0;
static pthread_mutex_t mutex;
static useconds_t delay = 1000; // us

static const char short_options[] = "hd:";
static const struct option
        long_options[] = {
        { "help",   no_argument,       NULL, 'h' },
        { "delay",  required_argument, NULL, 'd' },
        { 0, 0, 0, 0 }
};

static void errno_exit(const char *s)
{
    fprintf(stderr, "%s error %d, %s\\n", s, errno, strerror(errno));
    exit(EXIT_FAILURE);
}

static void usage(FILE *fp, int argc, char **argv)
{
    fprintf(fp,
            "Usage: %s [options]\n\n"
            "Version 1.0\n"
            "Options:\n"
            "-h | --help          Print this message\n"
            "-d | --delay         Delay microseconds before write\n",
            argv[0]);
}

static void parse_args(int argc, char **argv)
{
    for (;;) {
        int idx;
        int c;

        c = getopt_long(argc, argv, short_options, long_options, &idx);

        if (-1 == c)
            break;

        switch (c) {
            case 0: /* getopt_long() flag */
                break;
            case 'd':
                delay = atoi(optarg);
                break;
            case 'h':
            default:
                usage(stderr, argc, argv);
                exit(EXIT_FAILURE);
        }
    }
}

static void signal_treatment(int signo)
{
    ALOGD("signo = %d", signo);

    pthread_mutex_lock(&mutex);
    running = 0;
    pthread_mutex_unlock(&mutex);
}

static void init_signal()
{
    signal(SIGINT, signal_treatment);
    signal(SIGTERM, signal_treatment);
    signal(SIGQUIT, signal_treatment);
}

int main(int argc, char **argv)
{
    int fd;
#define BUF_SIZE   1024
    uint8_t buf[BUF_SIZE];
    int ret;

    parse_args(argc, argv);

    pthread_mutex_init(&mutex, NULL);
    running = 1;
    init_signal();

    fd = uart_open(UART_PATH, UART_BAUDRATE, 0);
    if (fd < 0) {
        ALOGE("uart_open failed");
        return 0;
    }

    while(1) {
        pthread_mutex_lock(&mutex);
        if (!running) {
            pthread_mutex_unlock(&mutex);
            break;
        }
        pthread_mutex_unlock(&mutex);

        ret = uart_read(fd, buf, sizeof(buf));
        ALOGV("read: len = %d", ret);
        if (ret > 0) {
            if (delay > 0)
                usleep(delay);
            ret = uart_write(fd, buf, ret);
            ALOGV("write: len = %d", ret);
        }
    }

    uart_close(fd);
    pthread_mutex_destroy(&mutex);

    ALOGD("EXIT");
    return 0;
}