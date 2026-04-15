-- ShakePro Seed Data V2
-- 插入测试数据

-- 测试用户 (密码: test123456, BCrypt encoded)
INSERT INTO `users` (`username`, `password_hash`, `nickname`) VALUES
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户');

-- 鸡尾酒样例数据
INSERT INTO `cocktails` (`name`, `description`, `image_url`, `alcohol_level`, `steps`) VALUES
('莫吉托 Mojito', '经典古巴鸡尾酒，以白朗姆酒为基底，搭配青柠和薄荷，清爽怡人。', 'https://images.unsplash.com/photo-1551538827-9c037cb4f32a?w=400', 15,
'1. 将6-8片薄荷叶放入杯中，加入15ml糖浆\n2. 用捣棒轻轻捣碎薄荷叶释放香气\n3. 加入30ml新鲜青柠汁\n4. 加入45ml白朗姆酒\n5. 加满碎冰\n6. 倒入苏打水至满杯\n7. 轻轻搅拌，用薄荷叶和青柠片装饰'),

('玛格丽特 Margarita', '墨西哥经典鸡尾酒，龙舌兰酒与青柠的完美结合，杯口的盐边是点睛之笔。', 'https://images.unsplash.com/photo-1556855810-ac404aa91e85?w=400', 25,
'1. 用青柠片蘸湿杯口，将杯口倒扣在盐盘上做盐边\n2. 在摇酒壶中加入45ml龙舌兰酒\n3. 加入20ml君度橙酒\n4. 加入25ml新鲜青柠汁\n5. 加入冰块，用力摇匀10-15秒\n6. 过滤倒入做好盐边的鸡尾酒杯中\n7. 用青柠角装饰'),

('长岛冰茶 Long Island Iced Tea', '看似冰茶，实则烈性十足，由多种基酒混合而成的经典派对饮品。', 'https://images.unsplash.com/photo-1536935338788-846bb9981813?w=400', 40,
'1. 在高杯中加入冰块\n2. 依次加入15ml伏特加、15ml白朗姆酒、15ml金酒、15ml龙舌兰酒\n3. 加入15ml君度橙酒\n4. 加入25ml新鲜柠檬汁\n5. 加入15ml糖浆\n6. 注入可乐至满杯\n7. 轻搅，用柠檬片装饰'),

('老式鸡尾酒 Old Fashioned', '威士忌鸡尾酒的鼻祖，简约而不简单，品味经典。', 'https://images.unsplash.com/photo-1470337458703-46ad1756a187?w=400', 35,
'1. 在威士忌杯中放入1块方糖\n2. 滴入2-3滴安格仕苦精\n3. 加入少许苏打水，捣碎方糖\n4. 加入60ml波本威士忌\n5. 加入大冰块\n6. 轻轻搅拌30秒\n7. 用橙皮扭转挤油后投入杯中，加樱桃装饰'),

('椰林飘香 Pina Colada', '热带风情鸡尾酒，椰奶与菠萝汁的梦幻组合。', 'https://images.unsplash.com/photo-1587223962217-fdded5940a99?w=400', 12,
'1. 在搅拌机中加入45ml白朗姆酒\n2. 加入60ml菠萝汁\n3. 加入30ml椰奶\n4. 加入一杯碎冰\n5. 高速搅拌至顺滑\n6. 倒入飓风杯中\n7. 用菠萝角和樱桃装饰'),

('Cosmopolitan 大都会', '时尚优雅的经典鸡尾酒，蔓越莓的红色诱惑。', 'https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?w=400', 22,
'1. 将摇酒壶加入冰块冷却\n2. 加入40ml柑橘伏特加\n3. 加入15ml君度橙酒\n4. 加入15ml新鲜青柠汁\n5. 加入30ml蔓越莓汁\n6. 用力摇匀10秒\n7. 双重过滤倒入马天尼杯，用橙皮装饰'),

('威士忌酸 Whiskey Sour', '酸甜平衡的经典酸类鸡尾酒，蛋清带来丝滑口感。', 'https://images.unsplash.com/photo-1560512823-829485b8bf24?w=400', 20,
'1. 在摇酒壶中加入60ml波本威士忌\n2. 加入25ml新鲜柠檬汁\n3. 加入20ml糖浆\n4. 可选：加入一个蛋清\n5. 先干摇（不加冰）10秒\n6. 加入冰块，再摇匀10秒\n7. 过滤倒入杯中，用柠檬片和樱桃装饰');

-- 材料数据
INSERT INTO `materials` (`name`, `category`) VALUES
-- 烈酒 Spirit
('白朗姆酒', 'spirit'),
('金朗姆酒', 'spirit'),
('伏特加', 'spirit'),
('金酒', 'spirit'),
('龙舌兰酒', 'spirit'),
('波本威士忌', 'spirit'),
('苏格兰威士忌', 'spirit'),
('君度橙酒', 'spirit'),
('柑橘伏特加', 'spirit'),
-- 果汁 Juice
('青柠汁', 'juice'),
('柠檬汁', 'juice'),
('菠萝汁', 'juice'),
('蔓越莓汁', 'juice'),
('橙汁', 'juice'),
-- 糖浆 Syrup
('糖浆', 'syrup'),
('蜂蜜糖浆', 'syrup'),
('方糖', 'syrup'),
-- 其他 Mixer
('苏打水', 'mixer'),
('可乐', 'mixer'),
('椰奶', 'mixer'),
('蛋清', 'mixer'),
-- 装饰 Garnish
('薄荷叶', 'garnish'),
('青柠片', 'garnish'),
('柠檬片', 'garnish'),
('橙皮', 'garnish'),
('樱桃', 'garnish'),
('菠萝角', 'garnish'),
-- 苦精 Bitters
('安格仕苦精', 'bitters');

-- 鸡尾酒-材料关系
-- Mojito
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(1, 1, '45ml'), (1, 10, '30ml'), (1, 15, '15ml'), (1, 18, '适量'), (1, 22, '8片'), (1, 23, '2片');

-- Margarita
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(2, 5, '45ml'), (2, 8, '20ml'), (2, 10, '25ml');

-- Long Island Iced Tea
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(3, 3, '15ml'), (3, 1, '15ml'), (3, 4, '15ml'), (3, 5, '15ml'), (3, 8, '15ml'), (3, 11, '25ml'), (3, 15, '15ml'), (3, 19, '适量');

-- Old Fashioned
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(4, 6, '60ml'), (4, 17, '1块'), (4, 28, '2-3滴'), (4, 18, '少许'), (4, 25, '1片'), (4, 26, '1颗');

-- Pina Colada
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(5, 1, '45ml'), (5, 12, '60ml'), (5, 20, '30ml'), (5, 27, '1角'), (5, 26, '1颗');

-- Cosmopolitan
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(6, 9, '40ml'), (6, 8, '15ml'), (6, 10, '15ml'), (6, 13, '30ml'), (6, 25, '1片');

-- Whiskey Sour
INSERT INTO `cocktail_materials` (`cocktail_id`, `material_id`, `amount`) VALUES
(7, 6, '60ml'), (7, 11, '25ml'), (7, 15, '20ml'), (7, 21, '1个'), (7, 24, '1片'), (7, 26, '1颗');
