create table game_db.cards
(
    id          int                                       not null comment '卡片唯一ID'
        primary key,
    name        varchar(100)                              not null comment '卡片名称',
    card_type   enum ('CHARACTER', 'ADDICTION', 'ACTION') not null comment '卡片类型：角色卡/附属卡/行动卡',
    create_time timestamp default CURRENT_TIMESTAMP       null comment '创建时间'
)
    comment '所有卡片的基础信息';

create table game_db.character_cards
(
    card_id int         not null comment '角色卡ID（关联cards表的id）'
        primary key,
    element varchar(20) not null comment '元素类型（如：岩、水、火等）',
    hp      int         not null comment '生命值',
    energy  int         not null comment '充能',
    constraint character_cards_ibfk_1
        foreign key (card_id) references game_db.cards (id)
            on delete cascade
)
    comment '角色卡特有信息';

create table game_db.player
(
    player_id         varchar(20)                        not null comment '玩家唯一标识'
        primary key,
    player_name       varchar(32)                        not null comment '玩家昵称',
    password          varchar(255)                       not null comment '加密后的密码（BCrypt）',
    registration_time datetime default CURRENT_TIMESTAMP not null comment '注册时间',
    last_login_time   datetime                           null comment '最后登录时间',
    status            int      default 0                 not null comment '账户状态（0-离线，1-上线）',
    current_room_id   varchar(20)                        null comment '玩家加入的房间号',
    constraint idx_player_name
        unique (player_name)
)
    comment '玩家基础信息表';

create table game_db.battles
(
    battleId    int auto_increment
        primary key,
    room_id     varchar(20) not null,
    player_id1  varchar(20) not null,
    player_id2  varchar(20) not null,
    turn        int         not null comment '当前回合数',
    our_card1   int         null comment '我方出战卡牌1',
    our_card2   int         null comment '我方出战卡牌2',
    our_card3   int         null comment '我方出战卡牌3',
    enemy_card1 int         null comment '对方出战卡牌1',
    enemy_card2 int         null comment '对方出战卡牌2',
    enemy_card3 int         null comment '对方出战卡牌3',
    winner      varchar(20) null comment '获胜者玩家ID',
    loser       varchar(20) null comment '失败者玩家ID',
    status      int         null comment '对局状态',
    constraint battles_ibfk_1
        foreign key (player_id1) references game_db.player (player_id),
    constraint battles_ibfk_10
        foreign key (winner) references game_db.player (player_id),
    constraint battles_ibfk_11
        foreign key (loser) references game_db.player (player_id),
    constraint battles_ibfk_2
        foreign key (player_id2) references game_db.player (player_id),
    constraint battles_ibfk_4
        foreign key (our_card1) references game_db.character_cards (card_id),
    constraint battles_ibfk_5
        foreign key (our_card2) references game_db.character_cards (card_id),
    constraint battles_ibfk_6
        foreign key (our_card3) references game_db.character_cards (card_id),
    constraint battles_ibfk_7
        foreign key (enemy_card1) references game_db.character_cards (card_id),
    constraint battles_ibfk_8
        foreign key (enemy_card2) references game_db.character_cards (card_id),
    constraint battles_ibfk_9
        foreign key (enemy_card3) references game_db.character_cards (card_id)
);

create index enemy_card1
    on game_db.battles (enemy_card1);

create index enemy_card2
    on game_db.battles (enemy_card2);

create index enemy_card3
    on game_db.battles (enemy_card3);

create index loser
    on game_db.battles (loser);

create index our_card1
    on game_db.battles (our_card1);

create index our_card2
    on game_db.battles (our_card2);

create index our_card3
    on game_db.battles (our_card3);

create index playerId1
    on game_db.battles (player_id1);

create index playerId2
    on game_db.battles (player_id2);

create index winner
    on game_db.battles (winner);

