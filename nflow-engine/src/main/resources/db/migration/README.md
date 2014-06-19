See http://flywaydb.org/documentation/migration/ and http://flywaydb.org/documentation/migration/sql.html

- Add directory per database engine
- Naming convention for files:
  V<release number>_<sequence number>__<description>.sql
 -- Release number is the next release, e.g 0.3.0
 -- sequence number starts from one, and resets back 1 on new release
 -- there are two (2) underscores after the sequence number
 -- files are applied in order based on filename
 -- for example: 
 V0.2.0_1__first_script.sql
 V0.2.0_2__second_script.sql
 V0.2.0_3__third_script.sql
 V0.3.0_1__fourth_script.sql
 V0.3.0_2__fifth_script.sql
