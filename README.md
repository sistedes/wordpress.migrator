# SISTEDES Digital Library Wordpress migrator

Utility to migrate from the legacy Wordpress-based SISTEDES digital Library to the OJS platform.

NOTE: This is a work in progress.

# Usage

```
usage: java -jar <this-file.jar> -u <base url> [-c <conference>] [-s
       <start-year>] [-e <end-year>] [-o <output file>] [-d <delay>]
 -u,--url <base url>             Base URL of the Wordpress SISTEDES
                                 Digital Library
 -c,--conferences <conference>   Limit the migration to the specified
                                 conferences (optional, process all
                                 conferences by default)
 -s,--start-year <start-year>    Consider only editions celebrated after
                                 the specified year including it
                                 (optional, start from the oldest by
                                 default)
 -e,--end-year <end-year>        Consider only editions celebrated before
                                 the specified year including it
                                 (optional, end at the lastest by default)
 -o,--output <output file>       The output file (optional, stdout will be
                                 used if no file is specified)
 -d,--delay-long <delay>         Time to wait (in ms) between connections
                                 to the Sistedes Digital Library to avoid
                                 flooding it (optional, no delay if not
                                 set)
```

**Note:** a `exceptions.txt` file will be created in the working directory that can be used in subsequent executions to manually specify exceptions on how names and surnames are detected when parsing publications authors.