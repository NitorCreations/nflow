create table base (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(30),
 time1 time not null
);

create table identical (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(30),
 time1 time not null
);

create table more_columns (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(30),
 time1 time not null,
 extra char(1)
);

create table wrong_columns (
 id integer not null primary key,
 text1 varchar(20),
 text_wrong char(30),
 time1 time not null,
 extra char(1)
);

create table fewer_columns (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(30)
);

create table wrong_type (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(30),
 time1 integer not null
);

create table smaller_size (
 id integer not null primary key,
 text1 varchar(20),
 text2 char(25),
 time1 time not null
);

create table larger_size (
 id integer not null primary key,
 text1 varchar(25),
 text2 char(30),
 time1 time not null
);