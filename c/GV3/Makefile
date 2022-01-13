CC=gcc
CFLAGS=-Wall -O3 -Wno-unused-result

all: test_amb_speed

test_amb_speed: spooky.h spooky.c csf.h csf.c csf3.h csf3.c test_amb_speed.c
	$(CC) $(CFLAGS) test_amb_speed.c csf3.c csf.c spooky.c -o test_amb_speed -fopenmp
	
debug_amb: spooky.h spooky.c csf.h csf.c csf3.h csf3.c test_amb_speed.c
	$(CC) $(CFLAGS) -g test_amb_speed.c csf3.c csf.c spooky.c -o test_amb_speed
