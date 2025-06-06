create database if not exists `java_gobang`;

use `java_gobang`;

drop table if exists `user`;

create table `user` (
    `userId` int primary key auto_increment,
    `username` varchar(50),
    `password` varchar(50),
    `score` int, -- 积分
    `totalCount` int, -- 总对局数
    `winCount` int -- 赢的局数
);

insert into `user` values (null, 'zhangsan', '123456', 1000, 0, 0);
insert into `user` values (null,'lisi', '123456', 1000, 0, 0);
insert into `user` values (null,'wangwu', '123456', 1000, 0, 0);