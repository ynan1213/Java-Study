CREATE TABLE `stk_allocated`
(
    `id`               bigint                                  NOT NULL AUTO_INCREMENT,
    `sku_id`           bigint                                  NOT NULL,
    `stk_lpn_loc_id`   bigint                                           DEFAULT NULL COMMENT '库存主表ID',
    `lot_id`           bigint                                           DEFAULT NULL COMMENT '批次ID',
    `loc_id`           bigint                                           DEFAULT NULL COMMENT '分配库位ID',
    `lpn_no`           varchar(50) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '分配LPN号',
    `qty_allocated`    decimal(20, 3)                                   DEFAULT NULL COMMENT '分配库存',
    `warehouse_id`     int                                              DEFAULT NULL COMMENT '仓库ID',
    `type`             varchar(10) COLLATE utf8mb4_general_ci           DEFAULT NULL COMMENT '分配类型，PK-出库拣货，MV-移位，RP-补货，MW-移库',
    `version`          int                                              DEFAULT '0',
    `create_time`      datetime                                         DEFAULT CURRENT_TIMESTAMP,
    `create_by`        varchar(60) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `update_time`      datetime                                         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `update_by`        varchar(60) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    `is_deleted`       bigint                                           DEFAULT '0',
    `corporation_code` varchar(32) COLLATE utf8mb4_general_ci  NOT NULL DEFAULT '1014' COMMENT '主体编码',
    `corporation_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '上海雨生百谷食品有限公司' COMMENT '主体名称',
    `tenant_code`      varchar(32) COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'DDGY' COMMENT '租户编码',
    PRIMARY KEY (`id`) USING BTREE,
    KEY                `idx_stk_lpn_loc` (`stk_lpn_loc_id`) USING BTREE,
    KEY                `idx_update_time` (`update_time`) USING BTREE,
    KEY                `idx_sku_lot_loc_lpn` (`sku_id`,`lot_id`,`loc_id`,`lpn_no`,`warehouse_id`) USING BTREE,
    KEY                `idx_warehouse_id_lpn_no` (`warehouse_id`,`lpn_no`) USING BTREE,
    KEY                `idx_loc_id` (`loc_id`) USING BTREE,
    KEY                `index_0` (`warehouse_id`,`is_deleted`,`qty_allocated`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1172537346193678337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='已分配库存'