:warning: | **This tool is no longer needed, since the migration of the Sistedes Digital Library from Wordpress to DSpace took place in Sept. 2023** | :warning:
----------|-------------------------------------------------------------------------------------------------------------------------------|----------

# Sistedes Digital Library Wordpress migrator

**QUICK (not so quick) and DIRTY (quite dirty)** utility to migrate from the legacy Wordpress-based Sistedes Digital Library to the [DSpace 7.x](https://dspace.lyrasis.org/) platform.

**This is an _ad hoc_ implementation** for the Wordpress-based Sistedes Digital Library that has been hosted at https://biblioteca.sistedes.es from 2015 until 2023, **and is not intended to be reused in any way**.

The tool logs **A LOT** of information, so that errors in the source legacy digital library can be detected and fixed before the final migration.

# Usage

```
usage: java -jar <this-file.jar> -i <input-url> [-c <conference>] [-s
       <start-year>] [-e <end-year>] -o <output-url> -u <user> -p
       <password> [-w <delay>] -h <prefix> -k <key> [-x <key>] [-m] [-t]
       [-d]
 -i,--input <input-url>          Base URL of the Wordpress Sistedes
                                 Digital Library to read
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
 -o,--output <output-url>        Base URL of the DSpace Sistedes Digital
                                 Library to write
 -u,--user <user>                User of the DSpace Sistedes Digital
                                 Library with write privileges
 -p,--password <password>        Password of the user
 -w,--waiting-time <delay>       Time to wait (in ms) between connections
                                 to the Sistedes Digital Library to avoid
                                 flooding it (optional, no delay if not
                                 set)
 -h,--handle-prefix <prefix>     Prefix of the Handle registry to update
 -k,--handle-key-file <key>      File with the private key to athenticate
                                 in the Handle system
 -x,--handle-password <key>      Password to decypt the Handle key file
 -m,--migrate-docs               Also migrate the Sistedes documents to
                                 the target DSpace instance
 -t,--interactive                Ask interactively when there is
                                 uncertainty when matching authors
 -d,--dry-run                    Do not perform any changes in the target
                                 DSpace instance
```

**Note:** this program will write temporary and cache files in the working directories:

* A `exceptions.txt` file will be created so that it can be used in subsequent executions to manually specify exceptions on how names and surnames are detected when parsing publications authors.
* A `pdfcache` directory will be created so that PDF files of retrieved articles will be stored there and cached so that they won't be downloaded in subsequent executions.

Also, it expects to have a copy of the seminars files in a `videocache` directory. If no files for a seminar are found, it will be created as a `withdrawn` Seminar.