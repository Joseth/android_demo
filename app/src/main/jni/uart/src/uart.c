//
// Created by joseth on 20-3-25.
//
#define LOG_TAG "uart"
#define CONFIG_DEBUG

#include "uart.h"
#include <termios.h>
#include <fcntl.h>
#include <mc_log.h>
#include <unistd.h>

static speed_t getBaudrate(int baudrate)
{
    switch(baudrate) {
        case 0: return B0;
        case 50: return B50;
        case 75: return B75;
        case 110: return B110;
        case 134: return B134;
        case 150: return B150;
        case 200: return B200;
        case 300: return B300;
        case 600: return B600;
        case 1200: return B1200;
        case 1800: return B1800;
        case 2400: return B2400;
        case 4800: return B4800;
        case 9600: return B9600;
        case 19200: return B19200;
        case 38400: return B38400;
        case 57600: return B57600;
        case 115200: return B115200;
        case 230400: return B230400;
        case 460800: return B460800;
        case 500000: return B500000;
        case 576000: return B576000;
        case 921600: return B921600;
        case 1000000: return B1000000;
        case 1152000: return B1152000;
        case 1500000: return B1500000;
        case 2000000: return B2000000;
        case 2500000: return B2500000;
        case 3000000: return B3000000;
        case 3500000: return B3500000;
        case 4000000: return B4000000;
        default: return -1;
    }
}

int uart_open(char *path, int baudrate, int flags)
{
    int fd;
    speed_t speed;

    speed = getBaudrate(baudrate);
    if (speed == -1) {
        ALOGE("Invalid baudrate: %d", baudrate);
        return -1;
    }

    fd = open(path, O_RDWR | flags);
    if (fd == -1) {
        ALOGE("Can't open %s", path);
        return -1;
    }

    // Config
    struct termios cfg;
    ALOGD("Configuring serial port");
    if (tcgetattr(fd, &cfg))
    {
        ALOGE("tcgetattr() failed");
        close(fd);
        return -1;
    }

    cfmakeraw(&cfg);
    cfsetispeed(&cfg, speed);
    cfsetospeed(&cfg, speed);

    if (tcsetattr(fd, TCSANOW, &cfg))
    {
        ALOGE("tcsetattr() failed");
        close(fd);
        return -1;
    }

    ALOGD("%s success", __func__);
    return fd;
}

void uart_close(int fd)
{
    ALOGD("%s", __func__);
    if (fd >= 0)
        close(fd);
}

int uart_write(int fd, const uint8_t *buf, size_t len)
{
    ASSERT(fd >= 0, "Invalid FD");

    return write(fd, buf, len);
}

int uart_read(int fd, uint8_t *buf, size_t buf_len)
{
    ASSERT(fd >= 0, "Invalid FD");

    return read(fd, buf, buf_len);
}