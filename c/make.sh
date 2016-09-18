echo "gcc test.c -o test `pkg-config --cflags --libs glib-2.0`"
gcc test.c -std=c99 -Wformat-security -o test `pkg-config --cflags --libs glib-2.0`
