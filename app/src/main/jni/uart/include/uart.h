//
// Created by joseth on 20-3-25.
//

#ifndef ANDROID_DEMO_UART_H
#define ANDROID_DEMO_UART_H

#include <stdio.h> // FILE
#include <sys/types.h>
#ifdef __cplusplus
extern "C" {
#endif

int uart_open(char *path, int baudrate, int flags);
void uart_close(int fd);

int uart_write(int fd, const uint8_t *buf, size_t len);
int uart_read(int fd, uint8_t *buf, size_t buf_len);

#ifdef __cplusplus
}
#endif
#endif //ANDROID_DEMO_UART_H
