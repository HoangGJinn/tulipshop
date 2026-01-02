-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: tulipshop
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `cart_id` bigint NOT NULL,
  `stock_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  KEY `FKcwvshmk1ged03ccmgygsuwk5q` (`stock_id`),
  CONSTRAINT `FKcwvshmk1ged03ccmgygsuwk5q` FOREIGN KEY (`stock_id`) REFERENCES `product_stock` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (47,'2025-12-30 11:43:00.666743',1,3,38),(49,'2025-12-30 12:05:13.981649',3,1,61),(57,'2025-12-31 12:32:41.257923',1,1,76),(61,'2025-12-31 23:45:07.416771',1,2,74);
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK64t7ox312pqal3p7fg9o503c2` (`user_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
INSERT INTO `carts` VALUES (1,'2025-12-06 09:43:06.464607','2025-12-06 09:43:06.465608',1),(2,'2025-12-19 14:49:48.943616','2025-12-19 14:49:48.943616',2),(3,'2025-12-27 20:47:16.474605','2025-12-27 20:47:16.474605',3);
/*!40000 ALTER TABLE `carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `slug` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsaok720gsu4u2wrgbk10b5n8d` (`parent_id`),
  CONSTRAINT `FKsaok720gsu4u2wrgbk10b5n8d` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,7,'Áo Kiểu','ao-kieu'),(2,7,'Áo thun','ao-thun'),(3,NULL,'Đầm','dam'),(4,7,'áo khoác','o-khoc'),(5,4,'giáng sinh','ging-sinh'),(6,4,'áo khoác nỉ','o-khoc-n'),(7,NULL,'Áo','ao'),(8,NULL,'Quần','quan');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ai_response` text,
  `chat_room_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `message_type` enum('GENERAL_INQUIRY','ORDER_INQUIRY','POLICY_ADVICE','PRODUCT_RECOMMENDATION','TEXT') NOT NULL,
  `policy_advice` text,
  `seen` bit(1) DEFAULT NULL,
  `sender_type` enum('AI_BOT','CUSTOMER','SUPPORT_AGENT') NOT NULL,
  `sender_id` bigint DEFAULT NULL,
  `session_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  KEY `FK3cpkdtwdxndrjhrx3gt9q5ux9` (`session_id`),
  CONSTRAINT `FK3cpkdtwdxndrjhrx3gt9q5ux9` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_messages`
--

LOCK TABLES `chat_messages` WRITE;
/*!40000 ALTER TABLE `chat_messages` DISABLE KEYS */;
INSERT INTO `chat_messages` VALUES (1,NULL,NULL,'chào bạn','2025-12-31 23:48:30.139324','TEXT',NULL,_binary '\0','CUSTOMER',2,9),(2,'Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?',NULL,'Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?','2025-12-31 23:48:31.657049','TEXT',NULL,_binary '\0','AI_BOT',NULL,9),(3,NULL,NULL,'tôi cần bạn tư vấn size áo cho tôi nặng 36 kg','2025-12-31 23:49:50.182735','TEXT',NULL,_binary '\0','CUSTOMER',2,9),(4,'💡 Bảng size tham khảo (chung, có thể khác biệt nhẹ theo từng mẫu):\n- S: Vòng ngực 86, vai 38, dài 65\n- M: Vòng ngực 90, vai 40, dài 67\n- L: Vòng ngực 96, vai 42, dài 69\n- XL: Vòng ngực 102, vai 44, dài 71\nBạn cho mình xin thêm chiều cao + số đo vòng ngực/vòng eo (hoặc mẫu bạn muốn mua) để mình chốt size chính xác hơn nhé.\n\n\nMình gợi ý một vài mẫu phù hợp bên dưới, bạn xem giúp mình nhé.',NULL,'💡 Bảng size tham khảo (chung, có thể khác biệt nhẹ theo từng mẫu):\n- S: Vòng ngực 86, vai 38, dài 65\n- M: Vòng ngực 90, vai 40, dài 67\n- L: Vòng ngực 96, vai 42, dài 69\n- XL: Vòng ngực 102, vai 44, dài 71\nBạn cho mình xin thêm chiều cao + số đo vòng ngực/vòng eo (hoặc mẫu bạn muốn mua) để mình chốt size chính xác hơn nhé.\n\n\nMình gợi ý một vài mẫu phù hợp bên dưới, bạn xem giúp mình nhé.','2025-12-31 23:49:51.480319','PRODUCT_RECOMMENDATION','💡 Bảng size tham khảo (chung, có thể khác biệt nhẹ theo từng mẫu):\n- S: Vòng ngực 86, vai 38, dài 65\n- M: Vòng ngực 90, vai 40, dài 67\n- L: Vòng ngực 96, vai 42, dài 69\n- XL: Vòng ngực 102, vai 44, dài 71\nBạn cho mình xin thêm chiều cao + số đo vòng ngực/vòng eo (hoặc mẫu bạn muốn mua) để mình chốt size chính xác hơn nhé.\n',_binary '\0','AI_BOT',NULL,9),(5,NULL,NULL,'tao là bố m','2026-01-01 09:39:16.089613','TEXT',NULL,_binary '\0','CUSTOMER',2,13),(6,'Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?',NULL,'Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?','2026-01-01 09:39:21.483912','TEXT',NULL,_binary '\0','AI_BOT',NULL,13);
/*!40000 ALTER TABLE `chat_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_sessions`
--

DROP TABLE IF EXISTS `chat_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `customer_context` text,
  `customer_email` varchar(255) DEFAULT NULL,
  `customer_name` varchar(255) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `session_token` varchar(255) NOT NULL,
  `session_status` enum('ACTIVE','ARCHIVED','ENDED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt703kyd2aii73xrxjn5ex6dcv` (`session_token`),
  KEY `FK82ky97glaomlmhjqae1d0esmy` (`user_id`),
  CONSTRAINT `FK82ky97glaomlmhjqae1d0esmy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_sessions`
--

LOCK TABLES `chat_sessions` WRITE;
/*!40000 ALTER TABLE `chat_sessions` DISABLE KEYS */;
INSERT INTO `chat_sessions` VALUES (1,'2025-12-31 23:44:35.545301','',NULL,NULL,NULL,'0c9d58a8-ed6a-4008-be48-6886a0041eca','ACTIVE','2025-12-31 23:44:35.545446',2),(2,'2025-12-31 23:45:05.524900','',NULL,NULL,NULL,'4da6f6e7-1a11-4cc6-9158-935a6f6e1bde','ACTIVE','2025-12-31 23:45:05.524900',2),(3,'2025-12-31 23:45:19.305650','',NULL,NULL,NULL,'8ca54f39-9a38-4bca-adbb-744bc17dd00d','ACTIVE','2025-12-31 23:45:19.305650',2),(4,'2025-12-31 23:45:22.041504','',NULL,NULL,NULL,'fbfdf237-f332-4fb5-92d4-9c56c15cb232','ACTIVE','2025-12-31 23:45:22.041504',2),(5,'2025-12-31 23:45:47.805672','',NULL,NULL,NULL,'c0cabf60-2f7a-4256-8a9f-e11bf8afd50a','ACTIVE','2025-12-31 23:45:47.805672',2),(6,'2025-12-31 23:46:23.062450','',NULL,NULL,NULL,'7b72c2a3-3c91-4351-85d3-829c6008c0a7','ACTIVE','2025-12-31 23:46:23.062450',2),(7,'2025-12-31 23:46:25.294628','',NULL,NULL,NULL,'63d1bb46-96f0-42a9-81e5-e992ed58a5a9','ACTIVE','2025-12-31 23:46:25.294628',2),(8,'2025-12-31 23:46:34.626895','',NULL,NULL,NULL,'4204899a-e1d5-4ac4-992d-0aace6f97c6e','ACTIVE','2025-12-31 23:46:34.626895',2),(9,'2025-12-31 23:48:22.477320',' User: chào bạn | AI: Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn? User: tôi cần bạn tư vấn size áo cho tôi nặng 36 kg | AI: 💡 Bảng size tham khảo (chung, có thể khác biệt nhẹ theo từng mẫu):\n- S: Vòng ngực 86, vai 38, dài 65\n- M: Vòng ngực 90, vai 40, dài 67\n- L: Vòng ngực 96, vai 42, dài 69\n- XL: Vòng ngực 102, vai 44, dài 71\nBạn cho mình xin thêm chiều cao + số đo vòng ngực/vòng eo (hoặc mẫu bạn muốn mua) để mình chốt size chính xác hơn nhé.\n\n\nMình gợi ý một vài mẫu phù hợp bên dưới, bạn xem giúp mình nhé.',NULL,NULL,NULL,'41171ccf-e4c4-4bc2-9f87-e9ad6330087d','ACTIVE','2025-12-31 23:49:51.488455',2),(10,'2026-01-01 09:35:50.156892','',NULL,NULL,NULL,'495be426-8edb-4f96-ac6c-1e80fab04892','ACTIVE','2026-01-01 09:35:50.156892',2),(11,'2026-01-01 09:36:22.724024','',NULL,NULL,NULL,'b939cc06-713b-4131-918f-465c1506f4fd','ACTIVE','2026-01-01 09:36:22.724024',2),(12,'2026-01-01 09:38:27.915473','',NULL,NULL,NULL,'b1c00f2e-c0b7-4168-978a-85d73fba59d3','ACTIVE','2026-01-01 09:38:27.916475',2),(13,'2026-01-01 09:39:09.000535',' User: tao là bố m | AI: Mình đã nhận câu hỏi của bạn. Bạn có thể cho mình biết thêm: bạn đang quan tâm chính sách (đổi trả/bảo hành/vận chuyển/thanh toán) hay tư vấn size/sản phẩm nào để mình hỗ trợ đúng hơn?',NULL,NULL,NULL,'2b1d7f9d-2cb1-464b-b0aa-70c2cf14c23d','ACTIVE','2026-01-01 09:39:21.493819',2);
/*!40000 ALTER TABLE `chat_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message_recommended_products`
--

DROP TABLE IF EXISTS `message_recommended_products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_recommended_products` (
  `message_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  KEY `FKgmlxnql2u8ci7tomnc2u01y18` (`product_id`),
  KEY `FKjf45kl0i1yyck2hnv2aqeuqo9` (`message_id`),
  CONSTRAINT `FKgmlxnql2u8ci7tomnc2u01y18` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKjf45kl0i1yyck2hnv2aqeuqo9` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message_recommended_products`
--

LOCK TABLES `message_recommended_products` WRITE;
/*!40000 ALTER TABLE `message_recommended_products` DISABLE KEYS */;
INSERT INTO `message_recommended_products` VALUES (4,1),(4,2),(4,3);
/*!40000 ALTER TABLE `message_recommended_products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_reads`
--

DROP TABLE IF EXISTS `notification_reads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_reads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `read_at` datetime(6) NOT NULL,
  `notification_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmovgbjijrkn1ommxqwv16h5pp` (`user_id`,`notification_id`),
  KEY `idx_user_notification` (`user_id`,`notification_id`),
  KEY `idx_notification` (`notification_id`),
  CONSTRAINT `FKg4vrpg3nw9pe3fdskpba65rjp` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`),
  CONSTRAINT `FKnlh7pma2y4w8vti9wu17ynqk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_reads`
--

LOCK TABLES `notification_reads` WRITE;
/*!40000 ALTER TABLE `notification_reads` DISABLE KEYS */;
INSERT INTO `notification_reads` VALUES (1,'2025-12-30 14:56:52.938380',1,3),(2,'2025-12-30 15:17:30.898767',2,2),(3,'2025-12-30 15:25:39.549886',1,2),(4,'2025-12-30 15:26:39.601979',2,1),(5,'2025-12-30 17:57:47.984084',3,2),(6,'2025-12-30 18:09:28.553585',6,2),(7,'2025-12-30 21:48:40.664277',8,2),(8,'2025-12-30 21:48:45.089345',9,2),(9,'2025-12-30 21:48:47.814767',10,2),(10,'2025-12-30 22:26:33.063590',7,2),(11,'2025-12-30 22:26:33.063590',5,2),(12,'2025-12-31 12:46:05.601277',14,2),(13,'2025-12-31 13:44:39.423419',13,2),(14,'2025-12-31 13:44:41.325318',12,2),(15,'2025-12-31 13:45:42.117954',17,2);
/*!40000 ALTER TABLE `notification_reads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `target_url` varchar(500) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `type` enum('ORDER','PROMOTION','SYSTEM') NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'ai bắt được Nguyễn Hoàng Giáp sẽ được giảm 20%','2025-12-30 14:53:01.435471','https://res.cloudinary.com/diawi4gde/image/upload/v1767081178/tulip-fashion/mnozd5gi5nsqneii9s0m.png','https://www.facebook.com/hoanggiap1803','Mừng ngày tiêu diệt Giáp Xô','PROMOTION',NULL),(2,'Nghỉ để fix bug','2025-12-30 14:58:13.109789',NULL,NULL,'Nghỉ mùng 1','SYSTEM',NULL),(3,'Đơn hàng #34 đã được đặt thành công. Tổng giá trị: 370,200 VNĐ','2025-12-30 17:57:09.130202',NULL,'/orders/34','Đặt hàng thành công','ORDER',2),(5,'Đơn hàng #34 đã được xác nhận và đang được chuẩn bị.','2025-12-30 18:07:22.440126',NULL,'/orders/34','Đơn hàng đã được xác nhận','ORDER',2),(6,'Đơn hàng #34 đang trên đường giao đến bạn.','2025-12-30 18:08:15.387461',NULL,'/orders/34','Đơn hàng đang được giao','ORDER',2),(7,'Đơn hàng #35 đã được đặt thành công. Tổng giá trị: 675,000 VNĐ','2025-12-30 21:37:19.191095',NULL,'/orders/35','Đặt hàng thành công','ORDER',2),(8,'ae vô đây phá đi, pass là admin123','2025-12-30 21:39:34.733639','https://res.cloudinary.com/diawi4gde/image/upload/v1767105572/tulip-fashion/yi6yj1aruljl0gc4uvcy.jpg',NULL,'Ta là hacker','SYSTEM',NULL),(9,'Đơn hàng #35 đã được xác nhận và đang được chuẩn bị.','2025-12-30 21:39:44.777961',NULL,'/orders/35','Đơn hàng đã được xác nhận','ORDER',2),(10,'Đơn hàng #35 đang trên đường giao đến bạn.','2025-12-30 21:40:03.878439',NULL,'/orders/35','Đơn hàng đang được giao','ORDER',2),(11,'test nữa test mãi','2025-12-30 22:43:24.348507',NULL,NULL,'test','SYSTEM',NULL),(12,'Đơn hàng #36 đã được đặt thành công. Tổng giá trị: 195,000 VNĐ','2025-12-31 12:43:09.800324',NULL,'/orders/36','Đặt hàng thành công','ORDER',2),(13,'Đơn hàng #36 đã được xác nhận và đang được chuẩn bị.','2025-12-31 12:45:19.142425',NULL,'/orders/36','Đơn hàng đã được xác nhận','ORDER',2),(14,'Đơn hàng #36 đang trên đường giao đến bạn.','2025-12-31 12:45:23.794425',NULL,'/orders/36','Đơn hàng đang được giao','ORDER',2),(15,'Đơn hàng #37 đã được đặt thành công. Tổng giá trị: 59,000 VNĐ','2025-12-31 13:41:21.544736',NULL,'/orders/37','Đặt hàng thành công','ORDER',2),(16,'Đơn hàng #37 đã được xác nhận và đang được chuẩn bị.','2025-12-31 13:45:22.856160',NULL,'/orders/37','Đơn hàng đã được xác nhận','ORDER',2),(17,'Đơn hàng #37 đang trên đường giao đến bạn.','2025-12-31 13:45:26.958127',NULL,'/orders/37','Đơn hàng đang được giao','ORDER',2);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price_at_purchase` decimal(38,2) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `size_id` int DEFAULT NULL,
  `stock_id` bigint DEFAULT NULL,
  `variant_id` bigint DEFAULT NULL,
  `snap_price` decimal(38,2) DEFAULT NULL,
  `snap_product_name` varchar(255) DEFAULT NULL,
  `snap_thumbnail_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
  KEY `FK9t2qyxv7hnjv24ox49t7oyga2` (`size_id`),
  KEY `FKfuugdd42mpglyytfqn2es6ds3` (`stock_id`),
  KEY `FKemq71edpbn9wsxnxncfn1algp` (`variant_id`),
  CONSTRAINT `FK9t2qyxv7hnjv24ox49t7oyga2` FOREIGN KEY (`size_id`) REFERENCES `sizes` (`id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKemq71edpbn9wsxnxncfn1algp` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `FKfuugdd42mpglyytfqn2es6ds3` FOREIGN KEY (`stock_id`) REFERENCES `product_stock` (`id`),
  CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,870000.00,1,'6-XANH LAM-S',1,6,1,27,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(2,90000.00,1,'3-XANH LAM-S',2,3,1,21,9,90000.00,'Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg'),(3,260000.00,1,'8-đầu-M',3,8,2,38,14,260000.00,'Áo khoác giáng sinh noel','https://res.cloudinary.com/diawi4gde/image/upload/v1766112857/tulip-fashion/uiu3wfbunm4nmuuvbnpd.png'),(4,260000.00,1,'8-đầu-M',4,8,2,38,14,260000.00,'Áo khoác giáng sinh noel','https://res.cloudinary.com/diawi4gde/image/upload/v1766112857/tulip-fashion/uiu3wfbunm4nmuuvbnpd.png'),(5,36000.00,1,'9-đầu-M',5,9,2,46,16,36000.00,'Nguyễn Hoàng Giáp','https://res.cloudinary.com/diawi4gde/image/upload/v1765379113/tulip-fashion/qukz4p4ur6uej4fauw95.jpg'),(6,300000.00,1,'4-VÀNG KEM-S',6,4,1,17,7,300000.00,'Đầm ngắn cute','https://res.cloudinary.com/diawi4gde/image/upload/v1764675493/tulip-fashion/eyoxvuvwgqsfdk3de7rz.jpg'),(7,300000.00,1,'4-VÀNG KEM-S',7,4,1,17,7,300000.00,'Đầm ngắn cute','https://res.cloudinary.com/diawi4gde/image/upload/v1764675493/tulip-fashion/eyoxvuvwgqsfdk3de7rz.jpg'),(8,870000.00,1,'6-XANH LAM-M',8,6,2,28,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(9,870000.00,1,'6-XANH LAM-M',9,6,2,28,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(10,44000.00,1,'12-Xanh đậm-S',10,12,1,63,20,44000.00,'Áo mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg'),(11,44000.00,1,'12-Xanh đậm-L',11,12,3,61,20,44000.00,'Áo mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg'),(12,870000.00,1,'6-XANH LAM-S',12,6,1,27,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(13,316800.00,1,'CBSE112502MTR',13,2,2,7,3,316800.00,'Set áo hai dây nhún ngực phối cardigan','https://cdn.hstatic.net/products/1000197303/pro_trang___1__80b8672b51fb4bf9babf23d703646214_master.jpg'),(14,316800.00,1,'CBSE112502MDE',14,2,1,8,4,316800.00,'Set áo hai dây nhún ngực phối cardigan','https://cdn.hstatic.net/products/1000197303/pro_trang___1__80b8672b51fb4bf9babf23d703646214_master.jpg'),(15,36000.00,1,'9-chân-L',15,9,3,49,17,36000.00,'Nguyễn Hoàng Giáp','https://res.cloudinary.com/diawi4gde/image/upload/v1765379113/tulip-fashion/qukz4p4ur6uej4fauw95.jpg'),(16,44000.00,1,'12-Xanh đậm-L',16,12,3,61,20,44000.00,'Áo mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg'),(17,180000.00,1,'13-trắng-L',17,13,3,73,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(18,180000.00,1,'13-trắng-M',18,13,2,74,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(19,180000.00,1,'13-trắng-M',19,13,2,74,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(20,180000.00,1,'13-trắng-L',20,13,3,73,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(21,180000.00,1,'13-trắng-L',21,13,3,73,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(22,260000.00,1,'8-đầu-L',22,8,3,37,14,260000.00,'Áo khoác giáng sinh noel','https://res.cloudinary.com/diawi4gde/image/upload/v1766112857/tulip-fashion/uiu3wfbunm4nmuuvbnpd.png'),(23,1800000.00,1,'10-xanh đen-M',23,10,2,54,18,1800000.00,'Váy xếp ly không tay ','https://res.cloudinary.com/diawi4gde/image/upload/v1765866465/tulip-fashion/n8bx5adwsesqr7i6647r.jpg'),(24,44000.00,1,'12-Đỏ-S',24,12,1,69,22,44000.00,'Áo mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg'),(25,870000.00,1,'6-XANH LAM-M',25,6,2,28,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(26,660000.00,1,'7-TRẮNG-S',26,7,1,29,12,660000.00,'Đầm sọc bẹt vai smocking eo','https://res.cloudinary.com/diawi4gde/image/upload/v1766124187/tulip-fashion/hzrjbjtu2xmlzfy3r4vh.jpg'),(27,90000.00,1,'3-XANH LAM-XL',27,3,4,24,9,90000.00,'Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg'),(28,90000.00,1,'3-XANH LAM-XL',28,3,4,24,9,90000.00,'Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg'),(29,90000.00,1,'3-XANH LAM-XL',29,3,4,24,9,90000.00,'Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg'),(30,90000.00,1,'3-XANH LAM-XL',30,3,4,24,9,90000.00,'Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg'),(31,1800000.00,1,'10-xanh đen-M',31,10,2,54,18,1800000.00,'Váy xếp ly không tay ','https://res.cloudinary.com/diawi4gde/image/upload/v1765866465/tulip-fashion/n8bx5adwsesqr7i6647r.jpg'),(32,1800000.00,1,'10-xanh đen-M',32,10,2,54,18,1800000.00,'Váy xếp ly không tay ','https://res.cloudinary.com/diawi4gde/image/upload/v1765866465/tulip-fashion/n8bx5adwsesqr7i6647r.jpg'),(33,870000.00,1,'6-XANH LAM-M',33,6,2,28,11,870000.00,'áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg'),(34,355200.00,1,'AK-TRANG-S',34,1,1,1,1,355200.00,'Áo kiểu voan tay dài kèm hoa','https://cdn.hstatic.net/products/1000197303/pro_trang___1__6eda201ee5f948b3af240cc3187bdce5_master.jpg'),(35,660000.00,1,'7-TRẮNG-S',35,7,1,29,12,660000.00,'Đầm sọc bẹt vai smocking eo','https://res.cloudinary.com/diawi4gde/image/upload/v1766124187/tulip-fashion/hzrjbjtu2xmlzfy3r4vh.jpg'),(36,180000.00,1,'13-trắng-M',36,13,2,74,23,180000.00,'Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg'),(37,44000.00,1,'12-Xanh đậm-L',37,12,3,61,20,44000.00,'Áo mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg');
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `final_price` decimal(38,2) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `shipping_address` varchar(512) DEFAULT NULL,
  `shipping_price` decimal(38,2) DEFAULT NULL,
  `status` enum('CANCELLED','CONFIRMED','DELIVERED','PENDING','RETURNED','SHIPPING') DEFAULT NULL,
  `total_price` decimal(38,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `voucher_id` bigint DEFAULT NULL,
  `payment_status` enum('EXPIRED','FAILED','PENDING','SUCCESS') DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `vnp_txn_ref` varchar(255) DEFAULT NULL,
  `payment_expire_at` datetime(6) DEFAULT NULL,
  `payment_url` text,
  `order_code` varchar(255) DEFAULT NULL,
  `recipient_name` varchar(255) DEFAULT NULL,
  `recipient_phone` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  KEY `FKdimvsocblb17f45ikjr6xn1wj` (`voucher_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKdimvsocblb17f45ikjr6xn1wj` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'2025-12-13 13:30:22.774051',900000.00,'COD','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'PENDING',870000.00,'2025-12-13 13:30:22.774051',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2,'2025-12-13 13:39:11.661326',120000.00,'COD','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'PENDING',90000.00,'2025-12-13 13:39:11.661326',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3,'2025-12-19 14:50:56.361208',290000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'PENDING',260000.00,'2025-12-19 14:50:56.399202',2,NULL,'PENDING',NULL,'TULIP-19122025-3-0115',NULL,NULL,NULL,NULL,NULL),(4,'2025-12-19 14:51:48.145592',290000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'PENDING',260000.00,'2025-12-19 14:52:44.366213',2,NULL,'SUCCESS','15354575','TULIP-19122025-4-4621',NULL,NULL,NULL,NULL,NULL),(5,'2025-12-19 14:55:22.767270',66000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'PENDING',36000.00,'2025-12-19 14:55:55.381805',2,NULL,'SUCCESS','15354584','TULIP-19122025-5-4637',NULL,NULL,NULL,NULL,NULL),(6,'2025-12-25 15:28:16.843637',330000.00,'MOMO','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CANCELLED',300000.00,'2025-12-25 18:32:40.593207',2,NULL,'EXPIRED','4636151779','TULIP-25122025-6-0297','2025-12-25 15:43:17.345878','https://test-payment.momo.vn/v2/gateway/pay?t=TU9NT3xUVUxJUC0yNTEyMjAyNS02LTAyOTc&s=63c90e51464f41a4d8fb36fa288576953e98c89a5ab6ecc5ffee18c996b33538',NULL,NULL,NULL),(7,'2025-12-25 18:36:13.871661',330000.00,'COD','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'PENDING',300000.00,'2025-12-25 18:36:13.965659',1,NULL,'PENDING',NULL,'TULIP-25122025-7-1793',NULL,NULL,NULL,NULL,NULL),(8,'2025-12-25 19:21:38.375685',900000.00,'VNPAY','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'CANCELLED',870000.00,'2025-12-25 19:57:52.006160',1,NULL,'EXPIRED','15368644','TULIP-25122025-8-7555','2025-12-25 19:36:38.524410','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=90000000&vnp_Command=pay&vnp_CreateDate=20251225192138&vnp_CurrCode=VND&vnp_ExpireDate=20251225193638&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-25122025-8-7555&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-25122025-8-7555&vnp_Version=2.1.0&vnp_SecureHash=3d5eaca6b7beb366b0e26ea1583feef7d372d2d035a4d0ded882a7ef10e1601833a8bd46daf86f4769cbb37b26a054789cca3270edff72ab396679256f91077e',NULL,NULL,NULL),(9,'2025-12-25 20:07:00.493575',900000.00,'VNPAY','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'CONFIRMED',870000.00,'2025-12-25 20:07:25.838456',1,NULL,'SUCCESS','15368687','TULIP-25122025-9-8122','2025-12-25 20:22:00.711551','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=90000000&vnp_Command=pay&vnp_CreateDate=20251225200700&vnp_CurrCode=VND&vnp_ExpireDate=20251225202200&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-25122025-9-8122&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-25122025-9-8122&vnp_Version=2.1.0&vnp_SecureHash=fa0d8f3e90cde3bc8e80d6ede3c6ba49b24e046e7148c989863e145a0e4e24f92dcb5ca400a0e5b93202b4c1032a62f76f9b8aeb1defa4fe442b0f8ab38b0113',NULL,NULL,NULL),(10,'2025-12-27 20:36:34.451904',74000.00,'COD','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'PENDING',44000.00,'2025-12-27 20:36:34.562059',2,NULL,'PENDING',NULL,'TULIP-27122025-10-9824',NULL,NULL,NULL,NULL,NULL),(11,'2025-12-27 20:48:50.816209',74000.00,'VNPAY','1 vvn, Xã Hoàng Nông, Huyện Đại Từ, Tỉnh Thái Nguyên - SĐT: 0906661587 (Người nhận: Trùm Bom Hàng)',30000.00,'CONFIRMED',44000.00,'2025-12-27 20:49:36.762033',3,NULL,'SUCCESS',NULL,'TULIP-27122025-11-3319','2025-12-27 21:03:50.915433','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=7400000&vnp_Command=pay&vnp_CreateDate=20251227204850&vnp_CurrCode=VND&vnp_ExpireDate=20251227210350&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-11-3319&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-11-3319&vnp_Version=2.1.0&vnp_SecureHash=e7fb39e840b300e347a7d2d5e223435466dfa4339280d9afd1bf6bf1b408269e1825a8f99e696ccc0011332c91f274aaf391231956e8aa8047d1706800e3d179',NULL,NULL,NULL),(12,'2025-12-27 20:50:47.988584',900000.00,'VNPAY','1 vvn, Xã Hoàng Nông, Huyện Đại Từ, Tỉnh Thái Nguyên - SĐT: 0906661587 (Người nhận: Trùm Bom Hàng)',30000.00,'CONFIRMED',870000.00,'2025-12-27 20:51:01.471005',3,NULL,'SUCCESS',NULL,'TULIP-27122025-12-0587','2025-12-27 21:05:48.036760','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=90000000&vnp_Command=pay&vnp_CreateDate=20251227205048&vnp_CurrCode=VND&vnp_ExpireDate=20251227210548&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-12-0587&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-12-0587&vnp_Version=2.1.0&vnp_SecureHash=2bcfc2123e303112dbfb9cc5d1a39c418347574c7943787a339c6963e0d2340f196496350dd620291441477595b6de4939113ab85ef66fdfc8c4048347fbee4f',NULL,NULL,NULL),(13,'2025-12-27 20:58:52.459487',346800.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CANCELLED',316800.00,'2025-12-27 21:14:18.014867',2,NULL,'EXPIRED',NULL,'TULIP-27122025-13-6566','2025-12-27 21:13:52.577966','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=34680000&vnp_Command=pay&vnp_CreateDate=20251227205852&vnp_CurrCode=VND&vnp_ExpireDate=20251227211352&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-13-6566&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-13-6566&vnp_Version=2.1.0&vnp_SecureHash=b9f7c6a8fbf1d0ce1455ab5730f8f7e6ffdfc392ab6eccb57d1a640c4be43cb89797106591a24a6888639d7364d33d653a292fd99272bf677eb94232a89a9621',NULL,NULL,NULL),(14,'2025-12-27 21:10:43.751720',346800.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CANCELLED',316800.00,'2025-12-27 21:10:48.126322',2,NULL,'FAILED',NULL,'TULIP-27122025-14-5761','2025-12-27 21:25:43.837695','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=34680000&vnp_Command=pay&vnp_CreateDate=20251227211043&vnp_CurrCode=VND&vnp_ExpireDate=20251227212543&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-14-5761&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-14-5761&vnp_Version=2.1.0&vnp_SecureHash=7bb943b41e3a2586fe52e59b571bd039cd13ea9f3fb828fc81351577f5a135eacb9f4f4692a1cd731a56e2c459bdd611808781adfaa862c1047e09d08111d964',NULL,NULL,NULL),(15,'2025-12-27 21:11:36.650472',66000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CONFIRMED',36000.00,'2025-12-27 21:12:31.045956',2,NULL,'SUCCESS','15371690','TULIP-27122025-15-8901','2025-12-27 21:26:36.699001','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=6600000&vnp_Command=pay&vnp_CreateDate=20251227211136&vnp_CurrCode=VND&vnp_ExpireDate=20251227212636&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-15-8901&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-15-8901&vnp_Version=2.1.0&vnp_SecureHash=2e6aad9a9497e1b9de1fa7bcfde8e1403af2ff45a841ddbd8ec17f4ec7722efff19cf20279524311f0a34b67e7a86a3955ea2e1708bed92f29dee891f0736e88',NULL,NULL,NULL),(16,'2025-12-27 22:39:09.387566',74000.00,'COD','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CONFIRMED',44000.00,'2025-12-28 20:14:53.841643',2,NULL,'PENDING',NULL,'TULIP-27122025-16-4235',NULL,NULL,NULL,NULL,NULL),(17,'2025-12-27 23:05:55.978940',210000.00,'MOMO','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'CANCELLED',180000.00,'2025-12-28 10:35:31.936692',1,NULL,'EXPIRED',NULL,'TULIP-27122025-17-2452','2025-12-27 23:20:56.802679','https://test-payment.momo.vn/v2/gateway/pay?t=TU9NT3xUVUxJUC0yNzEyMjAyNS0xNy0yNDUy&s=b9f7180175e9ae75ef0991e9a36bbc7503ddd5050a9c6158a2be65112661cbe0',NULL,NULL,NULL),(18,'2025-12-27 23:09:21.454827',210000.00,'VNPAY','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'CANCELLED',180000.00,'2025-12-28 10:35:32.036704',1,NULL,'EXPIRED',NULL,'TULIP-27122025-18-1212','2025-12-27 23:24:21.517825','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=21000000&vnp_Command=pay&vnp_CreateDate=20251227230921&vnp_CurrCode=VND&vnp_ExpireDate=20251227232421&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-18-1212&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-18-1212&vnp_Version=2.1.0&vnp_SecureHash=71ea14ae70a8667d52a60023bfd0d7a43f7a8b03bbd79e34bd92603824267a95dce3c456a22c39483b91de4b4a4dd620c340e414d253acf1fa7e5350a82572e5',NULL,NULL,NULL),(19,'2025-12-27 23:11:52.394421',210000.00,'VNPAY','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'CONFIRMED',180000.00,'2025-12-27 23:12:15.667113',1,NULL,'SUCCESS','15371764','TULIP-27122025-19-2550','2025-12-27 23:26:52.456413','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=21000000&vnp_Command=pay&vnp_CreateDate=20251227231152&vnp_CurrCode=VND&vnp_ExpireDate=20251227232652&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-19-2550&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-19-2550&vnp_Version=2.1.0&vnp_SecureHash=46f32083a73e254dc25491bbfa220659edf8478c4197aa7e76601be8c50396924c9779c8672687f0b97fffe55a32fb99757c9d3b20ce7c2f98bc975b358a571f',NULL,NULL,NULL),(20,'2025-12-27 23:14:53.318758',210000.00,'COD','7 Nguyen Lam, Phường 6, Quận Gò Vấp, Thành phố Hồ Chí Minh - SĐT: 0907384925 (Người nhận: Tèo Văn Tí)',30000.00,'PENDING',180000.00,'2025-12-27 23:14:53.369678',1,NULL,'PENDING',NULL,'TULIP-27122025-20-3939',NULL,NULL,NULL,NULL,NULL),(21,'2025-12-27 23:16:30.975790',210000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CONFIRMED',180000.00,'2025-12-27 23:16:57.442793',2,NULL,'SUCCESS','15371766','TULIP-27122025-21-7472','2025-12-27 23:31:31.037781','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=21000000&vnp_Command=pay&vnp_CreateDate=20251227231631&vnp_CurrCode=VND&vnp_ExpireDate=20251227233131&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-27122025-21-7472&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-27122025-21-7472&vnp_Version=2.1.0&vnp_SecureHash=3b966dca6953c79d829e7d0310ed0f789b33c250b4ef41b2a9a5fb5264a59500c67b2ef24a6463f239272052d9e082fc0aff245e70c70ef088fb5520dd254bd7',NULL,NULL,NULL),(22,'2025-12-28 10:37:37.834723',290000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CANCELLED',260000.00,'2025-12-28 10:53:43.221353',2,NULL,'EXPIRED','15371981','TULIP-28122025-22-2703','2025-12-28 10:52:37.902720','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=29000000&vnp_Command=pay&vnp_CreateDate=20251228103737&vnp_CurrCode=VND&vnp_ExpireDate=20251228105237&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-28122025-22-2703&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-28122025-22-2703&vnp_Version=2.1.0&vnp_SecureHash=eaf83de73a751522220aba7962e5ae8ef3b335a2f8b9d1a001002b6231830bf9937df8b84e03517217fd760e6f796e6e0ffa6e7794e485a58b16cb34af46254b',NULL,NULL,NULL),(23,'2025-12-28 10:55:05.027157',1830000.00,'VNPAY','7 Nguyen Lam, Phường An Hưng, Thành phố Thanh Hóa, Tỉnh Thanh Hóa - SĐT: 0907384922 (Người nhận: Thành Vinh)',30000.00,'CANCELLED',1800000.00,'2025-12-28 20:09:15.688500',2,NULL,'EXPIRED','15372010','TULIP-28122025-23-0495','2025-12-28 11:10:05.109116','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=183000000&vnp_Command=pay&vnp_CreateDate=20251228105505&vnp_CurrCode=VND&vnp_ExpireDate=20251228111005&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-28122025-23-0495&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-28122025-23-0495&vnp_Version=2.1.0&vnp_SecureHash=eb467a66f62990f0b494ae472aac8a61e87b57d4361ee8a973e44401cf5b566180c30d94e5170bcf113d9f5ba760bf87af8405a5041735ac80324beb10608a50',NULL,NULL,NULL),(24,'2025-12-28 20:25:12.022216',90000.00,'COD','Đại học bách khoa hà nội, Phường Bạch Mai, Quận Hai Bà Trưng, Thành phố Hà Nội',46000.00,'DELIVERED',44000.00,'2025-12-28 20:26:39.701844',1,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-28122025-24-7239','Tèo Văn Tí','0907384925'),(25,'2025-12-28 21:07:35.378847',885000.00,'VNPAY',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'CONFIRMED',870000.00,'2025-12-28 21:10:40.761688',2,NULL,'SUCCESS','15372487',NULL,'2025-12-28 21:22:56.958138','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=88500000&vnp_Command=pay&vnp_CreateDate=20251228210756&vnp_CurrCode=VND&vnp_ExpireDate=20251228212256&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-28122025-25-5772&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-28122025-25-5772&vnp_Version=2.1.0&vnp_SecureHash=0885d3eb88980e03cd181d461446e52a890e9bef5bd36219171c6aef763a175390677c8c7425a042b9d71e05600659b42a0ec522b7625f0d913a25057e3a4d6e','TULIP-28122025-25-5772','Thành Vinh','0907384922'),(26,'2025-12-28 21:16:19.720335',706000.00,'COD','Đại học bách khoa hà nội, Phường Bạch Mai, Quận Hai Bà Trưng, Thành phố Hà Nội',46000.00,'CONFIRMED',660000.00,'2025-12-30 11:40:21.815683',1,NULL,'PENDING',NULL,NULL,NULL,NULL,'TULIP-28122025-26-3046','Tèo Văn Tí','0907384925'),(27,'2025-12-28 21:45:51.588675',136000.00,'COD','Đại học bách khoa hà nội, Phường Bạch Mai, Quận Hai Bà Trưng, Thành phố Hà Nội',46000.00,'DELIVERED',90000.00,'2025-12-28 21:48:02.231078',1,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-28122025-27-0789','Tèo Văn Tí','0907384925'),(28,'2025-12-28 22:12:29.358555',136000.00,'COD','Đại học bách khoa hà nội, Phường Bạch Mai, Quận Hai Bà Trưng, Thành phố Hà Nội',46000.00,'DELIVERED',90000.00,'2025-12-30 13:44:23.967029',1,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-28122025-28-8391','Tèo Văn Tí','0907384925'),(29,'2025-12-28 22:15:47.827932',105000.00,'VNPAY',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',90000.00,'2025-12-28 22:18:47.896387',2,NULL,'SUCCESS','15372585',NULL,'2025-12-28 22:31:09.021301','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10500000&vnp_Command=pay&vnp_CreateDate=20251228221609&vnp_CurrCode=VND&vnp_ExpireDate=20251228223109&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-28122025-29-7398&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-28122025-29-7398&vnp_Version=2.1.0&vnp_SecureHash=3e6cf851c97fa6b437f84ac17c60844581bdc652e14586ca7006cdf2f3dd69e58ab261d88bc2980e940ce065af241d0beb8dc396f3e4558288e2850716da4a58','TULIP-28122025-29-7398','Thành Vinh','0907384922'),(30,'2025-12-29 10:43:44.436227',120000.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',30000.00,'DELIVERED',90000.00,'2025-12-29 10:48:46.202435',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-29122025-30-8963','Thành Vinh','0907384922'),(31,'2025-12-29 11:05:01.719937',1815000.00,'VNPAY',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',1800000.00,'2025-12-29 11:08:20.699793',2,NULL,'SUCCESS','15373071',NULL,'2025-12-29 11:20:02.544559','https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=181500000&vnp_Command=pay&vnp_CreateDate=20251229110502&vnp_CurrCode=VND&vnp_ExpireDate=20251229112002&vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_Locale=vn&vnp_OrderInfo=Thanh+toan+don+hang+TULIP-29122025-31-5176&vnp_OrderType=250000&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8787%2Fv1%2Fapi%2Fvnpay%2Fpayment-callback&vnp_TmnCode=LZB3745X&vnp_TxnRef=TULIP-29122025-31-5176&vnp_Version=2.1.0&vnp_SecureHash=29c91922b9e41423c9cdaa02ef03d9e01d9c55eb169f70e99438f60f4d472cfaf9a603e79b0f20f88dd0129a45d5ea62b6467d40410b4384fda78a1883d6ede7','TULIP-29122025-31-5176','Thành Vinh','0907384922'),(32,'2025-12-29 11:15:27.461535',1815000.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',1800000.00,'2025-12-29 11:17:16.107734',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-29122025-32-6462','Thành Vinh','0907384922'),(33,'2025-12-30 11:02:31.575152',900000.00,'COD','1 vvn, Xã Hoàng Nông, Huyện Đại Từ, Tỉnh Thái Nguyên',30000.00,'CONFIRMED',870000.00,'2025-12-30 11:37:57.588035',3,NULL,'PENDING',NULL,NULL,NULL,NULL,'TULIP-30122025-33-9534','Trùm Bom Hàng','0906661587'),(34,'2025-12-30 17:57:09.075244',370200.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',355200.00,'2025-12-30 18:08:45.571407',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-30122025-34-7960','Thành Vinh','0907384922'),(35,'2025-12-30 21:37:19.135327',675000.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',660000.00,'2025-12-30 21:40:33.979902',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-30122025-35-0139','Thành Vinh','0907384922'),(36,'2025-12-31 12:43:09.736354',195000.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',180000.00,'2025-12-31 12:45:53.919368',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-31122025-36-5319','Thành Vinh','0907384922'),(37,'2025-12-31 13:41:21.497742',59000.00,'COD',' The Sun Avenue, Phường Bình Thọ, Thành phố Thủ Đức, Thành phố Hồ Chí Minh',15000.00,'DELIVERED',44000.00,'2025-12-31 13:45:57.085039',2,NULL,'SUCCESS',NULL,NULL,NULL,NULL,'TULIP-31122025-37-4855','Thành Vinh','0907384922');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `persistent_logins`
--

DROP TABLE IF EXISTS `persistent_logins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `persistent_logins` (
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `series` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_used` timestamp NOT NULL,
  PRIMARY KEY (`series`),
  KEY `idx_persistent_logins_username` (`username`),
  KEY `idx_persistent_logins_last_used` (`last_used`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `persistent_logins`
--

LOCK TABLES `persistent_logins` WRITE;
/*!40000 ALTER TABLE `persistent_logins` DISABLE KEYS */;
/*!40000 ALTER TABLE `persistent_logins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_audit`
--

DROP TABLE IF EXISTS `product_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `change_type` varchar(255) DEFAULT NULL,
  `changed_at` datetime(6) NOT NULL,
  `changed_by` varchar(255) NOT NULL,
  `new_name` varchar(255) DEFAULT NULL,
  `new_price` decimal(38,2) DEFAULT NULL,
  `old_name` varchar(255) DEFAULT NULL,
  `old_price` decimal(38,2) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_audit`
--

LOCK TABLES `product_audit` WRITE;
/*!40000 ALTER TABLE `product_audit` DISABLE KEYS */;
INSERT INTO `product_audit` VALUES (1,'NAME_CHANGE','2025-12-31 11:37:20.640815','admin@local','Áo khoác xanh dành cho dân genshin',NULL,'Áo khoác xanh lam dành cho dân genshin',NULL,3),(2,'NAME_CHANGE','2025-12-31 11:55:06.207041','admin@local','Áo trắng hcmute',NULL,'Áo hcmute',NULL,13),(3,'NAME_CHANGE','2025-12-31 12:46:49.483351','admin@local','Áo trắng thể chất hcmute',NULL,'Áo trắng hcmute',NULL,13),(4,'NAME_CHANGE','2025-12-31 13:45:12.267657','admin@local','Áo xấu mặc ở nhà',NULL,'Áo mặc ở nhà',NULL,12);
/*!40000 ALTER TABLE `product_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_stock`
--

DROP TABLE IF EXISTS `product_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_stock` (
  `quantity` int DEFAULT NULL,
  `size_id` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `variant_id` bigint NOT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcyksx5gfnd2nhe4tvpy87dlhy` (`sku`),
  KEY `FKir6lc9skegnx0knabtcaxqwv` (`size_id`),
  KEY `FKd07balgs4xx0uysd0sh6ws96p` (`variant_id`),
  CONSTRAINT `FKd07balgs4xx0uysd0sh6ws96p` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`),
  CONSTRAINT `FKir6lc9skegnx0knabtcaxqwv` FOREIGN KEY (`size_id`) REFERENCES `sizes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_stock`
--

LOCK TABLES `product_stock` WRITE;
/*!40000 ALTER TABLE `product_stock` DISABLE KEYS */;
INSERT INTO `product_stock` VALUES (23,1,1,1,'AK-TRANG-S',NULL),(31,2,2,1,'AK-TRANG-M',NULL),(17,3,3,1,'AK-TRANG-L',NULL),(0,4,4,1,'AK-TRANG-XL',NULL),(0,1,5,2,'AK-DEN-S',NULL),(10,2,6,2,'AK-DEN-M',NULL),(20,2,7,3,'CBSE112502MTR',NULL),(36,1,8,4,'CBSE112502MDE',NULL),(0,1,13,6,'3-ĐEN-S',NULL),(9,2,14,6,'3-ĐEN-M',NULL),(5,3,15,6,'3-ĐEN-L',NULL),(0,4,16,6,'3-ĐEN-XL',NULL),(1,1,17,7,'4-VÀNG KEM-S',NULL),(0,2,18,7,'4-VÀNG KEM-M',NULL),(0,1,19,8,'4-ĐEN-S',NULL),(0,2,20,8,'4-ĐEN-M',NULL),(4,1,21,9,'3-XANH LAM-S',NULL),(0,2,22,9,'3-XANH LAM-M',NULL),(0,3,23,9,'3-XANH LAM-L',NULL),(3,4,24,9,'3-XANH LAM-XL',NULL),(8,1,27,11,'6-XANH LAM-S',NULL),(16,2,28,11,'6-XANH LAM-M',NULL),(33,1,29,12,'7-TRẮNG-S',NULL),(1,2,30,12,'7-TRẮNG-M',NULL),(1,3,31,12,'7-TRẮNG-L',NULL),(1,4,32,12,'7-TRẮNG-XL',NULL),(0,1,33,13,'7-XANH DƯƠNG-S',NULL),(0,2,34,13,'7-XANH DƯƠNG-M',NULL),(0,3,35,13,'7-XANH DƯƠNG-L',NULL),(0,4,36,13,'7-XANH DƯƠNG-XL',NULL),(20,3,37,14,'8-đầu-L',NULL),(8,2,38,14,'8-đầu-M',NULL),(0,1,39,14,'8-đầu-S',NULL),(5,4,40,14,'8-đầu-XL',NULL),(0,3,41,15,'9-tay-L',NULL),(2,2,42,15,'9-tay-M',NULL),(0,1,43,15,'9-tay-S',NULL),(0,4,44,15,'9-tay-XL',NULL),(0,3,45,16,'9-đầu-L',NULL),(2,2,46,16,'9-đầu-M',NULL),(0,1,47,16,'9-đầu-S',NULL),(0,4,48,16,'9-đầu-XL',NULL),(1,3,49,17,'9-chân-L',NULL),(0,2,50,17,'9-chân-M',NULL),(0,1,51,17,'9-chân-S',NULL),(0,4,52,17,'9-chân-XL',NULL),(0,3,53,18,'10-xanh đen-L',NULL),(18,2,54,18,'10-xanh đen-M',NULL),(10,1,55,18,'10-xanh đen-S',NULL),(0,4,56,18,'10-xanh đen-XL',NULL),(0,3,57,19,'11-đỏ-L',NULL),(0,2,58,19,'11-đỏ-M',NULL),(0,1,59,19,'11-đỏ-S',NULL),(0,4,60,19,'11-đỏ-XL',NULL),(2,3,61,20,'12-Xanh đậm-L',NULL),(0,2,62,20,'12-Xanh đậm-M',NULL),(4,1,63,20,'12-Xanh đậm-S',NULL),(1,4,64,20,'12-Xanh đậm-XL',NULL),(1,3,65,21,'12-đen-L',NULL),(2,2,66,21,'12-đen-M',NULL),(0,1,67,21,'12-đen-S',NULL),(0,4,68,21,'12-đen-XL',NULL),(2,1,69,22,'12-Đỏ-S',150000.00),(0,3,70,22,'12-Đỏ-L',NULL),(2,2,71,22,'12-Đỏ-M',NULL),(0,4,72,22,'12-Đỏ-XL',NULL),(15,3,73,23,'13-trắng-L',NULL),(10,2,74,23,'13-trắng-M',NULL),(10,3,75,24,'14-đen-L',NULL),(20,2,76,24,'14-đen-M',NULL);
/*!40000 ALTER TABLE `product_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_variant_images`
--

DROP TABLE IF EXISTS `product_variant_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_variant_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `variant_id` bigint NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9jwla8t7mpnllbt5gp0le008i` (`variant_id`),
  CONSTRAINT `FK9jwla8t7mpnllbt5gp0le008i` FOREIGN KEY (`variant_id`) REFERENCES `product_variants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_variant_images`
--

LOCK TABLES `product_variant_images` WRITE;
/*!40000 ALTER TABLE `product_variant_images` DISABLE KEYS */;
INSERT INTO `product_variant_images` VALUES (1,1,'https://cdn.hstatic.net/products/1000197303/pro_trang___1__6eda201ee5f948b3af240cc3187bdce5_master.jpg'),(2,1,'https://cdn.hstatic.net/products/1000197303/pro_trang___3__9f09b60c9fe94ce4bac164437049558f_master.jpg'),(3,2,'https://cdn.kkfashion.vn/6035-large_default/ao-voan-den-tay-dai-asm05-08.jpg'),(4,3,'https://cdn.hstatic.net/products/1000197303/pro_trang___1__80b8672b51fb4bf9babf23d703646214_master.jpg'),(5,3,'https://cdn.hstatic.net/products/1000197303/pro_trang___4__fba133aa63c64e16a3453b21e6b4f4e7_master.jpg'),(6,3,'https://cdn.hstatic.net/products/1000197303/pro_trang___5__d9208ee79be64492866740aa24134f94_master.jpg'),(7,4,'https://cdn.hstatic.net/products/1000197303/pro_den___6__342b79a808cc4706b411309cc668f90a_master.jpg'),(8,4,'https://cdn.hstatic.net/products/1000197303/pro_den___2__7e76565aaaeb4f3fb5d03db518c8fd32_master.jpg'),(9,4,'https://cdn.hstatic.net/products/1000197303/pro_den___4__cf4669eef73c4621b7b8e12633d54cc0_master.jpg'),(10,6,'https://res.cloudinary.com/diawi4gde/image/upload/v1764673864/tulip-fashion/drjofsysz88ws4ucsdy4.jpg'),(13,7,'https://res.cloudinary.com/diawi4gde/image/upload/v1764675560/tulip-fashion/f3nn8rgoohvte0b8rya9.jpg'),(14,7,'https://res.cloudinary.com/diawi4gde/image/upload/v1764675577/tulip-fashion/qawljsj84ambtpok5yua.jpg'),(15,7,'https://res.cloudinary.com/diawi4gde/image/upload/v1764675597/tulip-fashion/wh3xhhmmhz8duaeubptb.jpg'),(16,8,'https://res.cloudinary.com/diawi4gde/image/upload/v1764675612/tulip-fashion/lk9m2vgpbgrst0of6lpx.jpg'),(17,8,'https://res.cloudinary.com/diawi4gde/image/upload/v1764675625/tulip-fashion/kfdiuxcobdlu2nbycxhs.jpg'),(18,9,'https://res.cloudinary.com/diawi4gde/image/upload/v1764676424/tulip-fashion/mvtroso088vfoddrcn7v.jpg'),(19,9,'https://res.cloudinary.com/diawi4gde/image/upload/v1764676446/tulip-fashion/bxtxgp7bqqkbpe3wnlqu.jpg'),(20,11,'https://res.cloudinary.com/diawi4gde/image/upload/v1764919396/tulip-fashion/afx1ffdhgbin1hwjnmnt.jpg'),(21,12,'https://res.cloudinary.com/diawi4gde/image/upload/v1764934547/tulip-fashion/rh0ycrwcdlseimpbxco2.jpg'),(22,13,'https://res.cloudinary.com/diawi4gde/image/upload/v1764934556/tulip-fashion/lacuqp1pcwecqgyllcvk.jpg'),(23,15,'https://res.cloudinary.com/diawi4gde/image/upload/v1765379116/tulip-fashion/apblirhd9zhyehipknxr.png'),(24,15,'https://res.cloudinary.com/diawi4gde/image/upload/v1765379118/tulip-fashion/ayvwkujubv9jqbhp3qtn.png'),(25,16,'https://res.cloudinary.com/diawi4gde/image/upload/v1765379121/tulip-fashion/z9kahckzbd05nmgvce5u.png'),(26,17,'https://res.cloudinary.com/diawi4gde/image/upload/v1765379124/tulip-fashion/o4tgwisuisrimulib9vc.png'),(27,18,'https://res.cloudinary.com/diawi4gde/image/upload/v1765866465/tulip-fashion/n8bx5adwsesqr7i6647r.jpg'),(28,18,'https://res.cloudinary.com/diawi4gde/image/upload/v1765866827/84437958b_b_05_500_vaddqb.jpg'),(29,18,'https://res.cloudinary.com/diawi4gde/image/upload/v1765866827/84437958b_b_06_500_ndzwii.jpg'),(30,14,'https://res.cloudinary.com/diawi4gde/image/upload/v1766110551/tulip-fashion/w93qoey9jbqtlkrlmhhq.png'),(31,14,'https://res.cloudinary.com/diawi4gde/image/upload/v1766110553/tulip-fashion/amlkkwnh2kg56s3jjyys.png'),(32,14,'https://res.cloudinary.com/diawi4gde/image/upload/v1766110561/tulip-fashion/bzlkwyahxmgitqawzj8t.png'),(33,19,'https://res.cloudinary.com/diawi4gde/image/upload/v1766112741/tulip-fashion/bulzqayyxj2jtudbbqqi.jpg'),(34,19,'https://res.cloudinary.com/diawi4gde/image/upload/v1766112744/tulip-fashion/yfbruivcvhgoree9tj3k.jpg'),(35,20,'https://res.cloudinary.com/diawi4gde/image/upload/v1766715346/tulip-fashion/yc2y1kvgaovutbgmkgoz.jpg'),(36,20,'https://res.cloudinary.com/diawi4gde/image/upload/v1766715348/tulip-fashion/xnfxfwbxpjzjzqex9cy1.jpg'),(37,20,'https://res.cloudinary.com/diawi4gde/image/upload/v1766715350/tulip-fashion/t9pdi0ibdzjsxbugnynl.jpg'),(38,20,'https://res.cloudinary.com/diawi4gde/image/upload/v1766715352/tulip-fashion/s3pgrh4yzhdnpwihrheq.jpg'),(39,21,'https://res.cloudinary.com/diawi4gde/image/upload/v1766715435/tulip-fashion/wwypub2wj489kyuivna4.jpg'),(40,22,'https://res.cloudinary.com/diawi4gde/image/upload/v1766748233/tulip-fashion/koosmyymwyk4kp1qqvtj.jpg'),(41,23,'https://res.cloudinary.com/diawi4gde/image/upload/v1766851376/tulip-fashion/jswp6zolngb7emds51nv.jpg'),(42,24,'https://res.cloudinary.com/diawi4gde/image/upload/v1767158805/tulip-fashion/vov0oupeaognayx9lqop.jpg'),(43,24,'https://res.cloudinary.com/diawi4gde/image/upload/v1767158893/tulip-fashion/cke5q9duncsnzzkt1tf1.jpg'),(44,24,'https://res.cloudinary.com/diawi4gde/image/upload/v1767158981/tulip-fashion/rvojqrqoifb3suex6yh0.jpg'),(45,24,'https://res.cloudinary.com/diawi4gde/image/upload/v1767159068/tulip-fashion/g3rmhisowrehoiawog9y.jpg');
/*!40000 ALTER TABLE `product_variant_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_variants`
--

DROP TABLE IF EXISTS `product_variants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_variants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `color_code` varchar(255) DEFAULT NULL,
  `color_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKosqitn4s405cynmhb87lkvuau` (`product_id`),
  CONSTRAINT `FKosqitn4s405cynmhb87lkvuau` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_variants`
--

LOCK TABLES `product_variants` WRITE;
/*!40000 ALTER TABLE `product_variants` DISABLE KEYS */;
INSERT INTO `product_variants` VALUES (1,1,'#FFFFFF','Trắng'),(2,1,'#000000','Đen'),(3,2,'#FFFFFF','Trắng'),(4,2,'#000000','Đen'),(6,3,'#000000','Đen'),(7,4,'#edece7','Vàng Kem'),(8,4,'#000000','Đen'),(9,3,'#5d95c2','Xanh Lam'),(11,6,'#dcf1f4','Xanh Lam'),(12,7,'#ffffff','Trắng'),(13,7,'#0a1f8a','Xanh Dương'),(14,8,'#b32c2f','đỏ'),(15,9,'#3e65c4','tay'),(16,9,'#cea584','đầu'),(17,9,'#000000','chân'),(18,10,'#42474d','xanh đen'),(19,11,'#9b263b','đỏ'),(20,12,'#0b2f51','Xanh đậm'),(21,12,'#000000','đen'),(22,12,'#ff0000','Đỏ'),(23,13,'#ffffff','trắng'),(24,14,'#000000','đen');
/*!40000 ALTER TABLE `product_variants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `base_price` decimal(38,2) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) NOT NULL,
  `thumbnail` varchar(255) DEFAULT NULL,
  `discount_price` decimal(38,2) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `status` enum('ACTIVE','DELETED','HIDDEN') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (555000.00,1,1,'Áo kiểu voan tay dài kèm hoa mang đến vẻ đẹp nhẹ nhàng, nữ tính...','Áo kiểu voan tay dài kèm hoa','https://cdn.hstatic.net/products/1000197303/pro_trang___1__6eda201ee5f948b3af240cc3187bdce5_master.jpg',355200.00,'di-lam, di-tiec',NULL,'ACTIVE'),(495000.00,2,2,'<strong>Set Áo Hai Dây Nhún Ngực Phối Cardigan</strong></h3><p>Set áo hai dây nhún ngực phối cardigan mang đến phong cách nữ tính, nhẹ nhàng và thanh lịch. Áo nhún ngực tạo điểm nhấn mềm mại, kết hợp cardigan dễ phối giúp nàng thoải mái và tự tin trong mọi hoạt động.</p><p><strong>Đơn vị chịu trách nhiệm sản phẩm:</strong> Công ty Cổ Phần Sản Xuất Thương Mại Tâm Minh Phát<br><strong>Xuất xứ:</strong> Việt Nam<br><strong>Chất liệu:</strong> Thun kim cương<br><strong>Năm sản xuất:</strong> 2025</p><p><strong>Phù hợp để:</strong></p><ul><li>Đi chơi, dạo phố, hẹn hò</li><li>Cafe hoặc sinh hoạt cuối tuần cùng bạn bè</li><li>Phối cùng giày sneaker, sandal hoặc búp bê để tạo nhiều phong cách khác nhau</li></ul>','Set áo hai dây nhún ngực phối cardigan','https://cdn.hstatic.net/products/1000197303/pro_trang___1__80b8672b51fb4bf9babf23d703646214_master.jpg',316800.00,'di-choi',NULL,'ACTIVE'),(360000.00,4,3,'<p style=\"text-align: justify;\">L&agrave; trang bị thiết yếu trong tủ <a title=\"đồ nam\" href=\"https://www.coolmate.me/collection/do-nam\" target=\"_blank\" rel=\"noopener\">đồ nam</a> mỗi khi gi&oacute; về, <strong>&Aacute;o Phao D&agrave;y Ultrawarm Puffer C&oacute; Mũ II</strong> l&agrave; một \"ph&aacute;o đ&agrave;i\" di động, bảo vệ bạn một c&aacute;ch to&agrave;n diện. Đ&acirc;y kh&ocirc;ng chỉ l&agrave; một chiếc <a title=\"&aacute;o kho&aacute;c nam\" href=\"https://www.coolmate.me/collection/ao-khoac-nam\" target=\"_blank\" rel=\"noopener\">&aacute;o kho&aacute;c nam</a> th&ocirc;ng thường, m&agrave; l&agrave; mảnh gh&eacute;p chủ lực trong bộ sưu tập <a title=\"đồ thu đ&ocirc;ng\" href=\"https://www.coolmate.me/collection/do-thu-dong\" target=\"_blank\" rel=\"noopener\">đồ thu đ&ocirc;ng</a>, được thiết kế để đối mặt với những cơn gi&oacute; lạnh nhất. Mẫu <a title=\"&aacute;o phao\" href=\"https://www.coolmate.me/collection/ao-khoac-phao\" target=\"_blank\" rel=\"noopener\">&aacute;o phao</a>&nbsp;n&agrave;y được trang bị lớp l&oacute;t chần b&ocirc;ng c&ocirc;ng nghệ Ex-warm d&agrave;y dặn, c&ugrave;ng lớp vỏ 100% <a title=\"Polyester\" href=\"https://www.coolmate.me/blog/vai-polyester-la-gi\" target=\"_blank\" rel=\"noopener\">Polyester</a> c&oacute; khả năng chống gi&oacute; v&agrave; trượt nước, mang lại sự ấm &aacute;p tối đa m&agrave; vẫn giữ được n&eacute;t gọn g&agrave;ng, nhẹ t&ecirc;nh.</p>','Áo khoác xanh dành cho dân genshin','https://res.cloudinary.com/diawi4gde/image/upload/v1764673810/tulip-fashion/lmxjulsadvk8t56apr14.jpg',90000.00,'di-choi',NULL,'ACTIVE'),(360000.00,1,4,'xinh quá ghệ iu','Đầm ngắn cute','https://res.cloudinary.com/diawi4gde/image/upload/v1764675493/tulip-fashion/eyoxvuvwgqsfdk3de7rz.jpg',300000.00,'di-choi',NULL,'ACTIVE'),(900.00,2,5,'','áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919111/tulip-fashion/ygwf9qe2p4c2upi5znmr.jpg',870000.00,'di-choi','2025-12-31 11:35:41.928767','DELETED'),(900000.00,2,6,'','áo thun 2 dây phối ren ngực','https://res.cloudinary.com/diawi4gde/image/upload/v1764919160/tulip-fashion/sjldaykdfktzy3souxrv.jpg',870000.00,'di-choi',NULL,'ACTIVE'),(695000.00,3,7,'<h3><strong>Đầm sọc bẹt vai smocking eo</strong></h3>\r\n<p>Đầm bẹt vai với thiết kế smocking eo t&ocirc;n d&aacute;ng, tạo form nhẹ nh&agrave;ng v&agrave; nữ t&iacute;nh. Chất liệu linen sọc mềm mại, tho&aacute;ng m&aacute;t, mang lại cảm gi&aacute;c dễ chịu khi mặc v&agrave; ph&ugrave; hợp cho nhiều hoạt động hằng ng&agrave;y.</p>\r\n<p><strong>Đơn vị chịu tr&aacute;ch nhiệm sản phẩm:</strong> C&ocirc;ng ty Cổ Phần Sản Xuất Thương Mại T&acirc;m Minh Ph&aacute;t<br><strong>Xuất xứ:</strong> Việt Nam<br><strong>Chất liệu:</strong> linen sọc<br><strong>Năm sản xuất:</strong> 2025</p>\r\n<p><strong>Ph&ugrave; hợp để:</strong></p>\r\n<ul>\r\n<li>\r\n<p>mặc đi chơi, dạo phố, đi tiệc hoặc gặp đối t&aacute;c</p>\r\n</li>\r\n<li>\r\n<p>phối c&ugrave;ng gi&agrave;y b&uacute;p b&ecirc;, sandal, sneaker hoặc gi&agrave;y cao g&oacute;t t&ugrave;y phong c&aacute;ch</p>\r\n</li>\r\n<li>\r\n<p>tham gia c&aacute;c sự kiện nhẹ nh&agrave;ng, buổi gặp gỡ bạn b&egrave; hoặc picnic ngo&agrave;i trời</p>\r\n</li>\r\n</ul>','Đầm sọc bẹt vai smocking eo','https://res.cloudinary.com/diawi4gde/image/upload/v1766124187/tulip-fashion/hzrjbjtu2xmlzfy3r4vh.jpg',660000.00,'di-choi',NULL,'ACTIVE'),(500000.00,4,8,'<p style=\"text-align: right;\">mừng <em>ng&agrave;y</em> ch&uacute;a<strong> ra đời</strong></p>\r\n<h3><strong>NGUYỄN TH&Agrave;NH</strong> VINH&nbsp;</h3>\r\n<h1>Chiến thắng</h1>\r\n<p>Gi&aacute;p X&Ocirc; g&agrave;</p>\r\n<p><img src=\"https://res.cloudinary.com/diawi4gde/image/upload/v1766124002/tulip-fashion/lyoipywmzmvwjvazncqb.jpg\" alt=\"\" width=\"600\" height=\"450\"></p>','Áo khoác giáng sinh noel','https://res.cloudinary.com/diawi4gde/image/upload/v1766112857/tulip-fashion/uiu3wfbunm4nmuuvbnpd.png',260000.00,'di-choi,ao-khoac,giang-sinh',NULL,'ACTIVE'),(36000.00,2,9,'<p>gi&aacute;p x&ocirc;</p>','Nguyễn Hoàng Giáp','https://res.cloudinary.com/diawi4gde/image/upload/v1765379113/tulip-fashion/qukz4p4ur6uej4fauw95.jpg',36000.00,'di-tiec',NULL,'ACTIVE'),(2250000.00,3,10,'Không quá cầu kỳ về chi tiết, thiết kế đơn giản với váy xòe xếp ly vừa đủ để tôn lên nét thanh lịch và sang trọng của bạn trong ngày trọng đại.','Váy xếp ly không tay ','https://res.cloudinary.com/diawi4gde/image/upload/v1765866465/tulip-fashion/n8bx5adwsesqr7i6647r.jpg',1800000.00,'di-tiec',NULL,'ACTIVE'),(160000.00,3,11,'hihihi','áo hai dây giáng sinh','https://res.cloudinary.com/diawi4gde/image/upload/v1766112738/tulip-fashion/dbislapxpr2jgyj72g60.jpg',80000.00,'di-choi,noel','2025-12-31 13:48:28.852693','DELETED'),(50000.00,2,12,'<p>Th&agrave;nh phần: 95% vải cotton, 5% elastane</p>\r\n<p><img src=\"https://res.cloudinary.com/diawi4gde/image/upload/v1766715002/tulip-fashion/hspyzjoponnr0khgl9sh.jpg\" alt=\"\" width=\"316\" height=\"486\"></p>\r\n<p>&Aacute;o ph&ocirc;ng vải cotton co gi&atilde;n, d&aacute;ng slim fit. Cổ tr&ograve;n, cộc tay.<br><br>Mỏng nhẹ hơn so với mẫu Medium Weight cơ bản của ch&uacute;ng t&ocirc;i.</p>\r\n<p style=\"text-align: right;\"><img src=\"https://res.cloudinary.com/diawi4gde/image/upload/v1766715215/tulip-fashion/r6idxctukjpfhxfbt599.jpg\" alt=\"\" width=\"300\" height=\"486\"></p>','Áo xấu mặc ở nhà','https://res.cloudinary.com/diawi4gde/image/upload/v1766715344/tulip-fashion/e14zpesghdv8uaform68.jpg',44000.00,'o-nha,di-choi',NULL,'ACTIVE'),(200000.00,2,13,'<p>trường đại học c&ocirc;ng nghệ kỹ thuật</p>','Áo trắng thể chất hcmute','https://res.cloudinary.com/diawi4gde/image/upload/v1766851375/tulip-fashion/jwmy0s7f1xcmabmnbidx.jpg',180000.00,'di-hoc',NULL,'ACTIVE'),(239000.00,6,14,'<p class=\"QN2lPu\">😍😍😍 Shop CAM KẾT 😍😍😍</p>\r\n<p class=\"QN2lPu\">✔Về sản phẩm: Shop cam kết cả về CHẤT LIỆU cũng như H&Igrave;NH D&Aacute;NG (đ&uacute;ng với những g&igrave; được n&ecirc;u bật trong phần m&ocirc; tả sản phẩm).</p>\r\n<p class=\"QN2lPu\">✔Về gi&aacute; cả : Shop nhập với số lượng nhiều v&agrave; trực tiếp n&ecirc;n chi ph&iacute; sẽ l&agrave; RẺ NHẤT với chất lượng sản phẩm bạn nhận được.</p>\r\n<p class=\"QN2lPu\">✔Về dịch vụ: Shop sẽ cố gắng trả lời hết những thắc mắc xoay quanh sản phẩm nh&eacute;.</p>\r\n<p class=\"QN2lPu\">✔Thời gian chuẩn bị h&agrave;ng: H&agrave;ng c&oacute; sẵn, thời gian chuẩn bị tối ưu nhất.</p>\r\n<p style=\"text-align: right;\"><img src=\"https://res.cloudinary.com/diawi4gde/image/upload/v1767158319/tulip-fashion/hhiefwhp0sjpobv9ufpl.jpg\" alt=\"\" width=\"500\" height=\"667\"></p>','Ribbon sweeter set - Set áo nỉ lệch vai thêu nơ & chân váy bí ngắn cạp chun - Soulmate','https://res.cloudinary.com/diawi4gde/image/upload/v1767158717/tulip-fashion/gkvt7wqc8rk4mxtbaa84.jpg',203000.00,'di-choi',NULL,'ACTIVE');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating_images`
--

DROP TABLE IF EXISTS `rating_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(255) DEFAULT NULL,
  `rating_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK30swsrntej4tj641cf97tmyvm` (`rating_id`),
  CONSTRAINT `FK30swsrntej4tj641cf97tmyvm` FOREIGN KEY (`rating_id`) REFERENCES `ratings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating_images`
--

LOCK TABLES `rating_images` WRITE;
/*!40000 ALTER TABLE `rating_images` DISABLE KEYS */;
INSERT INTO `rating_images` VALUES (1,'https://res.cloudinary.com/diawi4gde/image/upload/v1764901890/drcg6nnjjiryicasn66e.jpg',1),(2,'https://res.cloudinary.com/diawi4gde/image/upload/v1765379121/tulip-fashion/z9kahckzbd05nmgvce5u.png',2);
/*!40000 ALTER TABLE `rating_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ratings`
--

DROP TABLE IF EXISTS `ratings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ratings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `stars` int NOT NULL,
  `variant_info` varchar(255) DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK228us4dg38ewge41gos8y761r` (`product_id`),
  KEY `FKb3354ee2xxvdrbyq9f42jdayd` (`user_id`),
  CONSTRAINT `FK228us4dg38ewge41gos8y761r` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKb3354ee2xxvdrbyq9f42jdayd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ratings`
--

LOCK TABLES `ratings` WRITE;
/*!40000 ALTER TABLE `ratings` DISABLE KEYS */;
INSERT INTO `ratings` VALUES (1,'em đẹp quá cho anh làm quen nhé','2025-11-29 19:43:38.317574',5,'Màu: Đen, Size: S',4,2),(2,'thằng giáp xô','2025-10-29 19:43:38.317574',2,'Màu: Vàng Kem, Size: M',4,3);
/*!40000 ALTER TABLE `ratings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked` bit(1) NOT NULL,
  `token` varchar(2000) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=130 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_tokens`
--

LOCK TABLES `refresh_tokens` WRITE;
/*!40000 ALTER TABLE `refresh_tokens` DISABLE KEYS */;
INSERT INTO `refresh_tokens` VALUES (1,'2025-12-17 13:54:08.380172','2025-12-24 13:54:08.380172',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NTk1NDQ0OCwiZXhwIjoxNzY2NTU5MjQ4fQ.mRrAlRd9JAaTjKyDcAPY5pRzQzR9HCFTxG3HjeuOqxfWWGOs5QZUTvv3IbD6ZqCsac9YBK_SokTJbPx_fp8W-w',2),(2,'2025-12-17 13:55:38.655811','2025-12-24 13:55:38.655811',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6NCwic3ViIjoiY29uY2FjYzU1N0BnbWFpbC5jb20iLCJpYXQiOjE3NjU5NTQ1MzgsImV4cCI6MTc2NjU1OTMzOH0.62JxW8o5m5MpwvP720pk70ZU_YowyC9ZXFNJ_RrEG9LAhOZX7hysNFoCpg5sPCbWaLpT-stlW2r1QgUGKDE8_g',4),(3,'2025-12-17 14:38:36.532021','2025-12-24 14:38:36.532021',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6NCwic3ViIjoiY29uY2FjYzU1N0BnbWFpbC5jb20iLCJpYXQiOjE3NjU5NTcxMTYsImV4cCI6MTc2NjU2MTkxNn0.-TRaCCIXCKJtIkkm0uY33eaz87riyeXFzEU0k2KP9oYVkxpwT2_992EOOu09VtAkqgOPWV7Y5qWcDC9GnGJBrg',4),(4,'2025-12-17 14:40:16.044553','2025-12-24 14:40:16.044553',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6NCwic3ViIjoiY29uY2FjYzU1N0BnbWFpbC5jb20iLCJpYXQiOjE3NjU5NTcyMTYsImV4cCI6MTc2NjU2MjAxNn0.Qih3RiqLsRH2D38ssgmHrePIeM8RlDInjQnBs8xFQRC4pOJgiQ9Skx1CEvYsYS0y2NIDTB-Jxup31LTuMPfw9g',4),(5,'2025-12-17 14:41:44.464192','2025-12-24 14:41:44.464192',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjU5NTczMDQsImV4cCI6MTc2NjU2MjEwNH0.Rs-lSC_3Xb8HXeIRcHo9cBDYEy-icdzpRh7flyOvmGvPXFEmsXTWw_fhZnzgqH9pJcuSq-fFBRzmLH89jRiUng',1),(6,'2025-12-19 08:30:14.253149','2025-12-26 08:30:14.253149',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMDc4MTQsImV4cCI6MTc2NjcxMjYxNH0.cKXTHEihFY6hJqH0rOZN3BCm3s3QYKOuLl2OzSW_skXQaiTxq0uTcmbR_PzqySvL2ok6u4Xwz8eAYe6NOJflyA',1),(7,'2025-12-19 09:08:51.310717','2025-12-26 09:08:51.309730',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMTAxMzEsImV4cCI6MTc2NjcxNDkzMX0.M7oVFNbekFXpkf8nUosP4QzV6_dcVFkNWxQpl-tVOp6yvnhfFf5g92YlFyXpj9zb1AiCM5ZcJvY6KDxATZxxRg',1),(8,'2025-12-19 11:36:32.880780','2025-12-26 11:36:32.880780',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMTg5OTIsImV4cCI6MTc2NjcyMzc5Mn0.lslzIA-rgIbtFUvw-i7NW9duWy5AXZZGBsRibleP_PMNCjW5GHNYP23N9xccvnRW3wdcg3vjjC61OZ98xtjDUw',1),(9,'2025-12-19 11:51:59.054065','2025-12-26 11:51:59.054065',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMTk5MTksImV4cCI6MTc2NjcyNDcxOX0.r-mATuKmH7fG59WH79WEZhBpFZBfeR-GH5nF0a5V1YHIOB_Yh6rBDvWUEVG1oqkI6Vah-FFVH6kipDUv6WJ2ww',1),(10,'2025-12-19 12:59:36.841043','2025-12-26 12:59:36.841043',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMjM5NzYsImV4cCI6MTc2NjcyODc3Nn0.cz6ts9wobd8d3btC9Vwit-P8t3dhrIkBLf8iN8WohxAP5VYXMD9Q1HWAmVuDQMHL7dbD1afFtRULqYPN0eLCOA',1),(11,'2025-12-19 14:49:31.626273','2025-12-26 14:49:31.626273',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjEzMDU3MSwiZXhwIjoxNzY2NzM1MzcxfQ.hCiKF6_DOFncllgdS9CPWACvJhrH-kpLrbpe0ucfotGD7nprNqAK6D5QvAMxP4eOodLCRVp-f__Jm1VNU71GmA',2),(12,'2025-12-19 14:56:43.020012','2025-12-26 14:56:43.020012',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjEzMTAwMywiZXhwIjoxNzY2NzM1ODAzfQ.ml-RgkDUpeTmKEYB_zxjZNS6GFgyn-P0QnuNPMEW2d5CLFP7KKtrf8G4fsbrQT3qq4r1G44nP0v8ADJ7HO5g4Q',2),(13,'2025-12-19 14:56:59.739252','2025-12-26 14:56:59.739252',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjYxMzEwMTksImV4cCI6MTc2NjczNTgxOX0.j8nDohje-KivGm15QfRIe7do7KmYlDGdcnkr2A5bOp_Rsr3gVZZz-7UmNhNiud-C6kkeuej8j1EqP0I4KVsREg',1),(14,'2025-12-24 13:54:44.034651','2025-12-31 13:54:44.034651',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjU1OTI4NCwiZXhwIjoxNzY3MTY0MDg0fQ.2lfIi4tsSd3cX2e7_nqQ1CFpsKxjCsXLTWDIbdegkg4lXKBGZ9zMNbI6YvOgQ92Y5G_g6FvTU_DK6XyiKiqywQ',2),(15,'2025-12-25 10:28:32.973976','2026-01-01 10:28:32.973976',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2MzMzMTIsImV4cCI6MTc2NzIzODExMn0.sogD1fZiRYpdJsSihhAK2Jt4grLpaEHk5TREQ2nWYWs6lUFmLZ3tAD0UQJ-4H0W3m5FRLjKbJnpgGsDI1tZUkQ',1),(16,'2025-12-25 11:07:21.280789','2026-01-01 11:07:21.280789',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2MzU2NDEsImV4cCI6MTc2NzI0MDQ0MX0.xsNdCelVKar-XA5n1-of9mJ1ILSP6CixOaVRrAYIBbQi5D31jkkaGuQbiQF_ENvgG1HKMrtjTSpjsauVpUdNfg',1),(17,'2025-12-25 15:22:02.289368','2026-01-01 15:22:02.289368',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NTA5MjIsImV4cCI6MTc2NzI1NTcyMn0.Ndlken1vJGIC_w1AelvY0ckWGReuyXToA6Yii72I0dSqqTR_BBrqHqz8hr16LtGSPRK-R5NFJTAWpTbWowG7kg',1),(18,'2025-12-25 15:27:18.132383','2026-01-01 15:27:18.132383',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjY1MTIzOCwiZXhwIjoxNzY3MjU2MDM4fQ.KYQaM4iHh1AOn-kD3KSkysYyLUvUb9KIdI6MEPhdPHwlBraIyv5vzZHMcj05jX1hYYptYf1hrMfsT6BiRA88qw',2),(19,'2025-12-25 15:30:47.066249','2026-01-01 15:30:47.066249',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NTE0NDcsImV4cCI6MTc2NzI1NjI0N30.oMksbJQMUTy1zGOjP08YJ4mZ7Xu-LDbeMhy0ey4xPNWfzZGo1BeqgsxE0afjx7s6IvHpqDlUurqk5cWGh_Uoqg',1),(20,'2025-12-25 18:34:16.439070','2026-01-01 18:34:16.439070',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NjI0NTYsImV4cCI6MTc2NzI2NzI1Nn0.KvvVsOJn9AHeJxsBN-kRNxIIw-ryF6YYo_WXJ5MEBtDl5Qt9K0GsFNACgFhdM4drg2DvQIY40kTLlpfo4jZ40Q',1),(21,'2025-12-25 18:36:51.551310','2026-01-01 18:36:51.551310',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjY2MjYxMSwiZXhwIjoxNzY3MjY3NDExfQ.TDVpCwMUagGrmBsv0RM1LKVG68F41EVfyQca1UFJHGQAZjqYS9LUpM0AHz-TfQ4B6VnnNVj1lOPWKuq5lBXpng',2),(22,'2025-12-25 19:19:56.039851','2026-01-01 19:19:56.039851',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NjUxOTYsImV4cCI6MTc2NzI2OTk5Nn0.4q9KAVUIYA4KFnz-H9keuAijlwwopEU7c9eHh6Aw-qVS8ubKbpQcWJXkCke_ewSgVZ2DbLSHd20hHLTEeq2xCw',1),(23,'2025-12-25 19:58:07.852888','2026-01-01 19:58:07.852888',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2Njc0ODcsImV4cCI6MTc2NzI3MjI4N30.cWZ0KiI9ErIIYjEnH0yDFPVxfubCHu7fcLnDH0qr1J6fR2_n5vCoC1ZET0Pe6Vzv2V7evuBS0jdtdMg50WT1mw',1),(24,'2025-12-25 20:15:22.826889','2026-01-01 20:15:22.826889',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2Njg1MjIsImV4cCI6MTc2NzI3MzMyMn0.sSUxxTRP6AeMHZYSlYKJRY_r6zpwtpwhVQhh7bpiqumNqNUs_luebsWA2HBTZELaaqhkQjXqc8Z7PpboJumDkQ',1),(25,'2025-12-25 20:42:53.608774','2026-01-01 20:42:53.608774',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NzAxNzMsImV4cCI6MTc2NzI3NDk3M30.fbT1A3t9HxceBicmMDuC1NQeNqVERNU3tHBA_YugSwiPiwIYaVQDEAeE91EhOtu7st45QEBtNkI8IXtLtZPDRw',1),(26,'2025-12-25 20:58:42.220211','2026-01-01 20:58:42.220211',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY2NzExMjIsImV4cCI6MTc2NzI3NTkyMn0.b7hZZ-LC2TobZgv1-QAG2dE0YT3iG1iqGTW7LapM68bVxGfJWXZqoLJX5Yi4s642UwI90t7vOs_1yB0tFx9teQ',1),(27,'2025-12-26 09:07:18.096810','2026-01-02 09:07:18.096810',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3MTQ4MzgsImV4cCI6MTc2NzMxOTYzOH0.Dz39hWLGvgRjm-LimlqfGq6bqqxhSvT4DMyPS6d2E9AsvBYlV26DHXLgESb2lYk2ZNDXSkn_W2umq90xY6NW2g',1),(28,'2025-12-26 12:20:07.980619','2026-01-02 12:20:07.980619',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3MjY0MDcsImV4cCI6MTc2NzMzMTIwN30.qs3r5ztNSVwpxLVyqJqSXRo_UzGFQA3wUmFyd0DaQ6sAt709oEg4bkBu1tjwEo7X3Ye_AV4N54NWSEz6So8KxA',1),(29,'2025-12-26 18:17:47.038573','2026-01-02 18:17:47.038573',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3NDc4NjYsImV4cCI6MTc2NzM1MjY2Nn0.AaghINApAJvBsGJ0LE2Wpv-cTB4w78S-djkirSbW9cjJyUfWAYTUiTbo7pmhq56ccFZ9IphDYQVH4sY3-DlEqQ',1),(30,'2025-12-26 18:47:00.476768','2026-01-02 18:47:00.476768',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3NDk2MjAsImV4cCI6MTc2NzM1NDQyMH0.3NihWM7H8KuqGIebOlY7ZY_j8GoLJVSYGEXsaD-I-HXQlGKOQmOnzTqHYNthi9N4mzWNhtsR09v8rCd344JG1A',1),(31,'2025-12-26 19:27:28.787156','2026-01-02 19:27:28.787076',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3NTIwNDgsImV4cCI6MTc2NzM1Njg0OH0.cLdWMFHuuYEYP9_pht-4fZxKS3hK6S5lQvQ7WuvrfzyO3RnX0x97y1R9GtmzUGTnYmCfOeoU1RQol_ouTSNRRQ',1),(32,'2025-12-26 20:26:29.440534','2026-01-02 20:26:29.440534',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY3NTU1ODksImV4cCI6MTc2NzM2MDM4OX0.LSWTEfsOT6f9uMrMm_S6OEWgI-epoJDGAhQ2QYuoIjGcAOEoL30R7bL6O88XrjJ3zgKBg-6Z2NNobrtuKfmhQQ',1),(33,'2025-12-27 20:35:19.082910','2026-01-03 20:35:19.082910',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg0MjUxOSwiZXhwIjoxNzY3NDQ3MzE5fQ.uV2D3SiI0fnM5sbj_JBeCcu4ocp5pZmeVavcsGIIb6ggSkODuiziCMrSHgCKZHwtbRPQVcTwq-G7JBVdg-sjXw',2),(34,'2025-12-27 20:46:32.575553','2026-01-03 20:46:32.575553',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY2ODQzMTkyLCJleHAiOjE3Njc0NDc5OTJ9.kHFYvzNL6jvPxJMA0tdIfPGTy53l3MB1MPJXdr6UgeCvH8noPkCPSWXP7H9jd_b52_T8x0XXmqYe_mIIhEzgMQ',3),(35,'2025-12-27 20:57:31.517211','2026-01-03 20:57:31.517211',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg0Mzg1MSwiZXhwIjoxNzY3NDQ4NjUxfQ.0Dt1PNX1BBa7Hi2PHjs9saG3rtymEhF0bca7-VXA0wv-qiSz86_cEEmsE4E0cxP6GzeX3qjAZnWwR4MWFHw3hA',2),(36,'2025-12-27 22:18:13.115208','2026-01-03 22:18:13.115208',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg0ODY5MywiZXhwIjoxNzY3NDUzNDkzfQ.ausputEuaaULGsNqarfDNCpkP97Ad8NLvQs2aZak1qiPGkIv4b0DqLhvARL5KTBsX5D5Bol39tgAWDa4PL_cfw',2),(37,'2025-12-27 22:40:34.642913','2026-01-03 22:40:34.642913',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY4NTAwMzQsImV4cCI6MTc2NzQ1NDgzNH0.2o77a6pApQWEADdzMjpOsePJ0ChfX_N3D67-Ud3PebP75M-hDY3CvYYs4iTwD0TgghSQzTQmjqYv870EyQ82IA',1),(38,'2025-12-27 23:16:16.042453','2026-01-03 23:16:16.042453',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg1MjE3NiwiZXhwIjoxNzY3NDU2OTc2fQ.sUUdXG3KA-VPrYg3rRsaaheJAoWq736lLi2vgk4ZlbGxBLfkvLFHBHF3bRJ57FzLK9cy1ycmsDF6D0GgyxYgzQ',2),(39,'2025-12-27 23:17:34.658595','2026-01-03 23:17:34.658595',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY4NTIyNTQsImV4cCI6MTc2NzQ1NzA1NH0.g6l6nS9dmN-U-_5qyrklH871ez0C7hCQZUeCFQs1k6ZMjO1ArWo-s1KjdsVHV14UcwsoCoezmsHypuUoRPYkHw',1),(40,'2025-12-28 10:36:45.179233','2026-01-04 10:36:45.179233',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg5MzAwNSwiZXhwIjoxNzY3NDk3ODA1fQ.o9pnkhcHJ70oEYkkU1FfBynjZVZMX5CxlgcE01X0oSSpm5x_v3YpqW6E-29wcfCIzqyad8jMyDeReD0yDpoaHw',2),(41,'2025-12-28 10:39:50.825668','2026-01-04 10:39:50.825668',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY4OTMxOTAsImV4cCI6MTc2NzQ5Nzk5MH0.VUtvRJLpWhUN4T3IXVoyOor4HCdZ6iE32wav9u-bO_eQ5riCqTbUVplax_TiYeRJq52eBQNLnS4bF2uwFjDBSQ',1),(42,'2025-12-28 10:54:21.630433','2026-01-04 10:54:21.630433',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njg5NDA2MSwiZXhwIjoxNzY3NDk4ODYxfQ.CUYnpp5439eDqDZZpaiR5zI9kfhCjqCq1OcckfIOEBJDdgvH0MGR8Kxn8IW2qsLe7kEVsaZRfJpwQr-2gNEzIQ',2),(43,'2025-12-28 10:56:23.951712','2026-01-04 10:56:23.951712',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY4OTQxODMsImV4cCI6MTc2NzQ5ODk4M30.OGstmp0BLyaDvROIC9taQueZCScGdNUj-cz_igvbV7oT2ShCoRqI7dg_DJk4GpxQ3ds9_xbrbmeZWeTG0zKAIQ',1),(44,'2025-12-28 20:14:14.665725','2026-01-04 20:14:14.665725',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5Mjc2NTQsImV4cCI6MTc2NzUzMjQ1NH0.LkJ3qVRW500pNAW4K93NC33YDgvuMoyQ3Tt_DbORUqUPBUt6J6OD2jwz6iyPchecPmhRSvX6v9CNzIMrDPgBwA',1),(45,'2025-12-28 21:00:23.153338','2026-01-04 21:00:23.152293',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzA0MjMsImV4cCI6MTc2NzUzNTIyM30.sNcaPbm8TNR31JXFFFEoGUXyZjhLq9JCkQIhgm0ZjaxVCuUZuxCNsm70BMRIyMPYjnK-jRQNEWKAMZiVOGCN-g',1),(46,'2025-12-28 21:01:32.476997','2026-01-04 21:01:32.476997',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjkzMDQ5MiwiZXhwIjoxNzY3NTM1MjkyfQ.vTZfSTzq6xOo3uk0xb-2dNOCXO4OB_MRQXk74J6NKLDgJIx47lOwYY6J_uFGX1eyOT8FeEjDnK2VzP8FKu_8BA',2),(47,'2025-12-28 21:09:26.618555','2026-01-04 21:09:26.617552',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzA5NjYsImV4cCI6MTc2NzUzNTc2Nn0.7I8uwbpCCiRN3RSgqKZ2Z5aO4XIbaHH3OdTNDokEwZ1FPh-ZqZokht51od1AfHZK4UesEkJiqqoXESTRdFzJUQ',1),(48,'2025-12-28 21:11:10.942076','2026-01-04 21:11:10.942076',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjkzMTA3MCwiZXhwIjoxNzY3NTM1ODcwfQ.08AeROR0yyt1N8bQgHUmCE1Uv2Sia07H0tqrRPJFcTB5lRH1hQP0oaM6Rq_MzIhS1JuI5rt5aMKksXw4nonMCQ',2),(49,'2025-12-28 21:11:29.345597','2026-01-04 21:11:29.345597',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzEwODksImV4cCI6MTc2NzUzNTg4OX0.nWqL8blMxCx5Du2qkcj86k_uPykqmoJhrJ1_JvPhnqyUwihFpJBKhqpQH3EAYo17LSW5PUGYk3ohPmlhJcilfQ',1),(50,'2025-12-28 21:14:27.087555','2026-01-04 21:14:27.087555',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzEyNjcsImV4cCI6MTc2NzUzNjA2N30.I1q4Gepjdacll1kg0AFyIRNr1DNWNY4eB1ii12tPVBG3SUOmHgtddp-q8Y3X-OYMDGBcQewTLWvW6szv9dtjkA',1),(51,'2025-12-28 22:13:31.504943','2026-01-04 22:13:31.504943',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjkzNDgxMSwiZXhwIjoxNzY3NTM5NjExfQ.BAkiP-UZ3ry7b8hg5h_Nieqy8PA2fXg1P-ms1MN_KEGvSxNFX_deIW6CLY6R5HXfWuzJUD7-pvBzPEArvKzTWg',2),(52,'2025-12-28 22:17:04.622268','2026-01-04 22:17:04.621449',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjkzNTAyNCwiZXhwIjoxNzY3NTM5ODI0fQ.VbEiuYrcLJHyLaQeR0Yb9TCCOyJXOMdq6TGsPsnsB6MwZS6lh1eneEWXeynNDVBPODH4hEx15UcJd8Keo3E9WQ',2),(53,'2025-12-28 22:17:14.976426','2026-01-04 22:17:14.976426',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzUwMzQsImV4cCI6MTc2NzUzOTgzNH0.y14keBWoB7SSkgf2iVDIDE5asG25leEz59zxrV8z8JQeAjyWPbK_EsOnqcr9pTJYQDmzrKnQdCt6AfImS8ydQw',1),(54,'2025-12-28 22:19:19.342992','2026-01-04 22:19:19.342992',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5MzUxNTksImV4cCI6MTc2NzUzOTk1OX0.alCcm1sBujJjXOLh253CbDNSVxf4Cc9bON0hEyH84g8wfdNZ6oPeavncvp3Je6jloYoSU5sTgsrOUF9xkM-iJw',1),(55,'2025-12-28 22:19:29.561117','2026-01-04 22:19:29.561117',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NjkzNTE2OSwiZXhwIjoxNzY3NTM5OTY5fQ.Y670dP61co4ZTDTCGBNFcp5ZHROnopAeGYfqu8qmq6BrvXb_zURFUUeR5yWy4P8i5_NeFVYPl-DG6U_QFHAI8g',2),(56,'2025-12-29 10:40:15.794182','2026-01-05 10:40:15.794182',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njk3OTYxNSwiZXhwIjoxNzY3NTg0NDE1fQ.iYgomqUpxrqnYykNxUCWoVm_-3R-n9pc-ibmZgA_BBAKC1vbFX4nkG4U90zwYR-xY9nnNapqLkuG9Ynf80e4Ag',2),(57,'2025-12-29 10:46:50.314545','2026-01-05 10:46:50.314545',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5ODAwMTAsImV4cCI6MTc2NzU4NDgxMH0.2jIfiFMACL3nA8ToBhScWXk4tlSWvhFcxSxqZnXMYaz37QJaYA6p9_HGz7dy23L_0I7t1wmgQK37mRwTgex48g',1),(58,'2025-12-29 10:49:47.208490','2026-01-05 10:49:47.208490',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njk4MDE4NywiZXhwIjoxNzY3NTg0OTg3fQ.aMGY4ABe7dXN7gkL89dt8gtdfybTrjj1upgv02fMLN8KAhd-w5mMmnxd2k_omorlmHjn3ry9wSmVTDcxp6Xwzg',2),(59,'2025-12-29 11:06:40.429084','2026-01-05 11:06:40.429084',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5ODEyMDAsImV4cCI6MTc2NzU4NjAwMH0.nVsDE3Mg3K0CSJzecEDK6wYmwDQoyhmWHEq8cBXD3tHd4_u-OtL3mblcQ7_1AuLqKnLG2T891Vwt8-u3IyAvjA',1),(60,'2025-12-29 11:14:10.071415','2026-01-05 11:14:10.071415',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2Njk4MTY1MCwiZXhwIjoxNzY3NTg2NDUwfQ.OEWcCntdwR7mro0PjyXGwy7CYTg5lDYkCgzJS71LxoGTzZdFfcpcuMPppTmnyPobyZpeWZc-x_SYKwQ_3nXhkQ',2),(61,'2025-12-29 11:16:16.584200','2026-01-05 11:16:16.584200',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjY5ODE3NzYsImV4cCI6MTc2NzU4NjU3Nn0.OzOhm_SgUXgQB8Xx8RpW4WfhA8EMo84w_0Gs844w7GwfYIpPqu1wcY6NBin_LxTrRM63SLk1M9lBsCjDTu-Vyg',1),(62,'2025-12-29 18:58:40.547918','2026-01-05 18:58:40.547918',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwMDk1MjAsImV4cCI6MTc2NzYxNDMyMH0.zAo_OfI-zyYZcUxXfz4prOULbqJw2sN5YitSsnfsT4Y78LWMqdGYQRSX0zyQ2RyDqiRvARyu3WnB7kFarCnrxg',1),(63,'2025-12-29 18:59:21.900983','2026-01-05 18:59:21.900983',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzAwOTU2MSwiZXhwIjoxNzY3NjE0MzYxfQ.5DyTmoZKbggo7tOTNMCfJYdiHolBsU17FHo9fBfpyD2sLDyLiDShSjc09LJLa0CEAD53Y4mUZxfGAdnxkGeNAg',2),(64,'2025-12-29 20:20:49.032554','2026-01-05 20:20:49.032554',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzAxNDQ0OSwiZXhwIjoxNzY3NjE5MjQ5fQ.tFvsg99Q3vD3VwdmDxkRa3myCH3XoUNDOxxqTtCdWAiCjSURxHV_PSQk8t1BE6_mMPIjk3cpzHORRFQOu8Wd4A',2),(65,'2025-12-29 20:29:35.107293','2026-01-05 20:29:35.107293',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwMTQ5NzUsImV4cCI6MTc2NzYxOTc3NX0.-mf6KFFj4-tT2zz3HgCEm3M4BBeh-WFhKLisUETw65CAbkK5H6JNK9Qdu3iUD-u8zfzJjFQxh6ZvIsIg2JPCUw',1),(66,'2025-12-29 21:33:06.486401','2026-01-05 21:33:06.486401',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzAxODc4NiwiZXhwIjoxNzY3NjIzNTg2fQ.dhP_RhIpfrVCHdMviFZBPtCo98s8XMIU3-fHyVazM7PSi2eu-zlVNcpeC43zLhTX-jf74RPo_6Cy2L8jAluOCw',2),(67,'2025-12-29 21:33:46.885568','2026-01-05 21:33:46.885568',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwMTg4MjYsImV4cCI6MTc2NzYyMzYyNn0.GgrtAp8pXRy_cRkHA8gYqzFau52LN8PaIR2Z0QUCIzTC4O9ZfVynSozTk-__QuqzMzv-gIctI3HyXu_plEelcQ',1),(68,'2025-12-29 22:11:32.166440','2026-01-05 22:11:32.166440',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzAyMTA5MiwiZXhwIjoxNzY3NjI1ODkyfQ.teElfCtFxT878R23TTSsb8VJ0hWYQDvellsAzEfA0fRYsaLOW0kAu7uw0MwQig3xCXJQRvkd-HArDxF9v1HAGA',2),(69,'2025-12-29 22:12:18.484693','2026-01-05 22:12:18.484693',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwMjExMzgsImV4cCI6MTc2NzYyNTkzOH0.0bHgMcwwf8k5dPKwqs-ooZMAbLunn2UAIkOvqIT5zn3hlKnCenJxDPMos12kvL_M_s6p8VYeiZ0dHVlZQr13hg',1),(70,'2025-12-30 10:11:52.445033','2026-01-06 10:11:52.445033',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA2NDMxMiwiZXhwIjoxNzY3NjY5MTEyfQ.jO5n1B6V4rJtzw0JrwI5xdbfXIL8N-TWSp9H4WT8xCTVeBkkZzRAPGH1AG1GsAFirMWjSb98-sff1eIxYO2xQg',2),(71,'2025-12-30 10:17:17.490071','2026-01-06 10:17:17.490071',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwNjQ2MzcsImV4cCI6MTc2NzY2OTQzN30.Uajc5702rELGQuCfqlSTb0pfzQVkuLYfHEExs5nPVCJyGu9uNw9RY2uUeCvIkWRP-s8F3KYhMo5pB_li3q4tmQ',1),(72,'2025-12-30 10:28:34.596951','2026-01-06 10:28:34.596951',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDY1MzE0LCJleHAiOjE3Njc2NzAxMTR9.0QWXfyW4lLu6Tg7tb3AmtOMMXjbAGq_0ZmZhzDu4rKfMJM574bNfR_I2Oi-Nn79ko3izT07hkbabFlS1t3W-zA',3),(73,'2025-12-30 10:33:10.398987','2026-01-06 10:33:10.398987',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDY1NTkwLCJleHAiOjE3Njc2NzAzOTB9.UDzxm6j0obnFU5PNwyZkGKuk8lHP0SftcLLIVjSqVYuZkCM86uZFNsrCh5KRY8Y-3-4JrzBIk6CKUjwik3IjCg',3),(74,'2025-12-30 11:33:49.703570','2026-01-06 11:33:49.703570',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDY5MjI5LCJleHAiOjE3Njc2NzQwMjl9.4Ho88hLn1aMr_to1rDuXEmLsCgH-Zoiy1o7kn65pVx5fXtChh8nmz4fJml3o1NtYn58O1kQfGWBC4-LFtCCuNg',3),(75,'2025-12-30 11:34:41.671180','2026-01-06 11:34:41.671180',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwNjkyODEsImV4cCI6MTc2NzY3NDA4MX0.-vc7wfNDLfuslkNZqMoTbiS5PjL4M_3z-EAo5zgGyePE5J_tpjPghMtEXxyrluiY1a6S2rciU-YaH-XQeXrJTw',1),(76,'2025-12-30 12:43:51.700441','2026-01-06 12:43:51.700441',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA3MzQzMSwiZXhwIjoxNzY3Njc4MjMxfQ.XusNsIMQS2JKDSpdAwH0NXwoisa6IEoLqJZoTpGyynl3DjCmrpemo7JhCjADIGklIPWHdyOAwMeMU_z0Kl0J6g',2),(77,'2025-12-30 13:25:17.780726','2026-01-06 13:25:17.780726',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwNzU5MTcsImV4cCI6MTc2NzY4MDcxN30.D3ikVr2SWfIx3C2wuG2BX6EBjlhNrNm43pc7QhrtJJq0_Ia2x0p4GBdV-C2G5n_J4CVthSTZsT1V4BMmSfg2gA',1),(78,'2025-12-30 13:59:52.924512','2026-01-06 13:59:52.924512',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA3Nzk5MiwiZXhwIjoxNzY3NjgyNzkyfQ.DN1xL9UP8E6Le9_LpfXb0pzL6L7gODMdc9Imdd7Ra1Ta5ftLu5aX6cOCmt1As0RowprFg-MarM3MuhiXJ6-82w',2),(79,'2025-12-30 14:00:17.061295','2026-01-06 14:00:17.061295',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwNzgwMTcsImV4cCI6MTc2NzY4MjgxN30.L9dNga0RU3rHN0lMsVbfq9zeQNQ-GRJeNxE2LTAM1Oz6_76vg2vxwkhHobbKmLrLo2OOyUhZ3ecGJ45hJTrEZA',1),(80,'2025-12-30 14:00:43.899390','2026-01-06 14:00:43.899390',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDc4MDQzLCJleHAiOjE3Njc2ODI4NDN9.pBmKq9CzyW0JIeX-2n7liqISMOIcY4XvUBx3Xzv5pNQ75Fvf3yN2aJifTS5LhF3-rJeIbOcPNaZxyw7X04JOdw',3),(81,'2025-12-30 14:01:06.925183','2026-01-06 14:01:06.925183',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDc4MDY2LCJleHAiOjE3Njc2ODI4NjZ9.sHzJKWo_gPiTcep0v2IrGJ707gYmNFSk9UGFVYTcjihiZX6M6LhE5FQePdHkO7hSdaBAk7ymo3ipBE09EcHNdg',3),(82,'2025-12-30 14:03:00.542244','2026-01-06 14:03:00.542244',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA3ODE4MCwiZXhwIjoxNzY3NjgyOTgwfQ.X7nagO5VDHg2s76VyFjirumHIFDBdcQLXu5ej15l-shmhGSfjTxDeXQYHIk17CJhFISUPI-iv-sJXGYD9B8lqQ',2),(83,'2025-12-30 14:03:28.589061','2026-01-06 14:03:28.589061',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwNzgyMDgsImV4cCI6MTc2NzY4MzAwOH0.1640Qo1hsQ5iwRKPkZIp-ZK4stD9nRG2ezFGOHw27-m52zYVmqRZDUTDE01ln5zngrQRR5ExKQqraBkMr-My0A',1),(84,'2025-12-30 14:50:48.469400','2026-01-06 14:50:48.469400',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDgxMDQ4LCJleHAiOjE3Njc2ODU4NDh9.8_dT9I19rHguwqMk44Iuw_yoAVHaA8dw0CYgygRU_fLjXQgeEt9Njac-ors7oZHw7yEZnoYaCr_gi0egP2Ujhg',3),(85,'2025-12-30 14:59:26.012321','2026-01-06 14:59:26.012321',_binary '\0','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Mywic3ViIjoiMjMxMTAxNzJAc3R1ZGVudC5oY211dGUuZWR1LnZuIiwiaWF0IjoxNzY3MDgxNTY1LCJleHAiOjE3Njc2ODYzNjV9.TCHcA4qsXGZfc6u-emyo-fzBr5NtfBr6FPqA8f_VNjquyjtnvztYOGlHtt_TwZiNqxAsu0HYf8h-vA3LYjsqpg',3),(86,'2025-12-30 15:03:35.108387','2026-01-06 15:03:35.108387',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA4MTgxNSwiZXhwIjoxNzY3Njg2NjE1fQ.0ECwf3ddbm86SPiqMpgB64dvQ9BOoO6Q2Q8hW7FxzRx5wnCLCKq5gTg-Ft98TqB7o2hgBwr7L9h3LEouPqCJ1Q',2),(87,'2025-12-30 15:26:09.493159','2026-01-06 15:26:09.493159',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwODMxNjksImV4cCI6MTc2NzY4Nzk2OX0.UdIvZZpOX0OTImL1S7tpKiQD-aVhbbcgzXj0RMtZ3fIIVxgBCULY9XU4jZtLCSxdeyHr-I4tI7nmMVlqaPHAtg',1),(88,'2025-12-30 17:54:14.535138','2026-01-06 17:54:14.535138',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5MjA1NCwiZXhwIjoxNzY3Njk2ODU0fQ.TfHYL0g7dXzewLLFthnly_UvdiL00TpeHzSSD-FCNlZ9eGhkfgJ-pNeyjAYfNk7u9olkQ9K3fpi2iOZkb_RCcg',2),(89,'2025-12-30 18:04:04.680850','2026-01-06 18:04:04.679850',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwOTI2NDQsImV4cCI6MTc2NzY5NzQ0NH0.w_yqMrbDJ3itQSluuvZlEJ6lvS5OuDJHg-G5UUdxO0gLlYnanwq2BbGZZP5Jv78mXXrofzKlezgJS-3U33RpOw',1),(90,'2025-12-30 18:06:40.264782','2026-01-06 18:06:40.264782',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5MjgwMCwiZXhwIjoxNzY3Njk3NjAwfQ.a61abebgLtPTasXH3cqVCCSPbWemudZruowNKQ_pF3IkAoaobGEkMYS4Ffgvcb2LaUUBhA-RpDy_jIkvqyaAYQ',2),(91,'2025-12-30 18:07:07.635151','2026-01-06 18:07:07.635151',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwOTI4MjcsImV4cCI6MTc2NzY5NzYyN30.0XLz8G0TfP-BPxhhaLzKrPkKleNNvVA3QvOo9QZLm_nPsQUBpfzqHMLvIhtPnaYE2cuGxu0G9f8rg4mPy2z34A',1),(92,'2025-12-30 18:07:35.037372','2026-01-06 18:07:35.037372',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5Mjg1NSwiZXhwIjoxNzY3Njk3NjU1fQ.PoNk1Q7F9rYvHk5ZCQAa_tIMKtvNiLl5tpaAQ-Ug-Flb9l28BBwWK134gvp3pJ8CFkLrVCd3VN4hcib5BDe91w',2),(93,'2025-12-30 18:08:02.824099','2026-01-06 18:08:02.824099',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcwOTI4ODIsImV4cCI6MTc2NzY5NzY4Mn0.5P4ylYdeqzP7GZ-gaJXZTxRdBivw-1Q86dF8_hTMcpyhfrGBUWkxVIdTqAiShwb7NdFKcpAgOzZh5rRtNSdXEA',1),(94,'2025-12-30 18:09:00.411315','2026-01-06 18:09:00.411315',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5Mjk0MCwiZXhwIjoxNzY3Njk3NzQwfQ.KqE7ZEDI_yu0uOTlCdgfz5CptmLkXVkLwX_NFpbOrXrZ-8OugIafYhn0h77kxfg4uDMczB7Ja9YgR4FFy9bvMg',2),(95,'2025-12-30 18:15:55.548905','2026-01-06 18:15:55.548905',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5MzM1NSwiZXhwIjoxNzY3Njk4MTU1fQ.afgfkr2D2aJKMIpZkUjlbMjbcQ6JZ1fJ6EmKiGR6vIRKbCC1pkBrJN2Zi-wKP0p5Mw0ijBYIeny1Ei-dE_yYrA',2),(96,'2025-12-30 19:25:10.687650','2026-01-06 19:25:10.687650',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzA5NzUxMCwiZXhwIjoxNzY3NzAyMzEwfQ.d9IF008o1m3Guu1L9GmT4CHC-1h3ty9wwXzRy5WYTeWRm944wctYiq40X4dqXbKemHC3gaD23qvdL7zxwl-jDQ',2),(97,'2025-12-30 20:42:42.487141','2026-01-06 20:42:42.486177',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwMjE2MiwiZXhwIjoxNzY3NzA2OTYyfQ.NhyxJS9bAl605mpj8iLv84-WqylAY-s-x1nDwiDPIeRrAsyx6HDflsdWrGkh_oHse4iztejeu7TJKXCqXtvb2A',2),(98,'2025-12-30 20:57:47.148228','2026-01-06 20:57:47.148228',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwMzA2NywiZXhwIjoxNzY3NzA3ODY3fQ._0DiOqfbAxVMAhqIoi5JvYw_D8dArAHbboBOff6IgQQS8fu7nIdH33LzDMS7tLPbK395P_gi5pNbWCTCqZZAzw',2),(99,'2025-12-30 21:38:06.965573','2026-01-06 21:38:06.965573',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxMDU0ODYsImV4cCI6MTc2NzcxMDI4Nn0.zPJ1CL24f2ssM2h5pNWsV-Hmgcn1Ne-_eScEH_VAIt725voftm0IzF_e3nKyLtstgzn94iSgrTTxD8LoL17r2Q',1),(100,'2025-12-30 21:40:20.414664','2026-01-06 21:40:20.414664',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwNTYyMCwiZXhwIjoxNzY3NzEwNDIwfQ.nwzzIaCPNbZc3jMwqsbk_dIMtf9Gks9KHiPmom1aOlc5ZFj088-qNnppqpfzTlvksWgTmeqpdvZsizDwEvoYEw',2),(101,'2025-12-30 22:18:01.227600','2026-01-06 22:18:01.227600',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwNzg4MSwiZXhwIjoxNzY3NzEyNjgxfQ.246RMKPA8xo6d73om9G2rNDqvXn44bkK1be1mFXVG6ukIAY45QLdX7cxUPsHJO1rWQAmzbxGYye-Q1-qA4v6Uw',2),(102,'2025-12-30 22:26:27.565670','2026-01-06 22:26:27.564669',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwODM4NywiZXhwIjoxNzY3NzEzMTg3fQ.Gnyza2N-aBe2ilKWEP6_nl5_lVsB2X0aGU3ndW6xtq0IHMfpkJlhr4eSRtw6izQfR8-8-qS115wn-wdorOm1Ng',2),(103,'2025-12-30 22:27:05.054445','2026-01-06 22:27:05.054445',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6NCwic3ViIjoiY29uY2FjYzU1N0BnbWFpbC5jb20iLCJpYXQiOjE3NjcxMDg0MjUsImV4cCI6MTc2NzcxMzIyNX0.rg8ZdhiYZATNFgs6_KIepuWruHGSXFtCrWU5Saq373JKtsFOMUYuVzwXJ7LrOp-X4c9UQu7w1CTRSTOFbI0_8g',4),(104,'2025-12-30 22:27:47.035599','2026-01-06 22:27:47.035599',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwODQ2NywiZXhwIjoxNzY3NzEzMjY3fQ._k_THvW5hROs3hoJ_RMovNKyYVODOQKQIZgSWilgoLDbiYX0gXkQf8MF7IDl32Sb6Vx4flKNYnLrTSMWND35qQ',2),(105,'2025-12-30 22:42:43.982424','2026-01-06 22:42:43.982424',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwOTM2MywiZXhwIjoxNzY3NzE0MTYzfQ.uOuJHCgcKV2q2m8vF6ka5b6KIAFrSd5CFBFFE0gi4cg60k93FeECRUtkZS9de4jm9TSLTcDMTWXRIqmsTYo-Dg',2),(106,'2025-12-30 22:42:59.658513','2026-01-06 22:42:59.658513',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxMDkzNzksImV4cCI6MTc2NzcxNDE3OX0.S535qWA1nLDt8e4V9NN7KHTvXynW_-bVrUN_XMULSpVI9UJRhF3UER9WmGmy08FMs1y7Agy-MYhLv8Baify9nQ',1),(107,'2025-12-30 22:43:35.861577','2026-01-06 22:43:35.861577',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwOTQxNSwiZXhwIjoxNzY3NzE0MjE1fQ.icGekOrlNIRIoAPc0_THfcvfLJxjsFdDQzjLKqiFBgIf6aEWegCksD_NZ0or2089rdctsDBsvTcBaJ8nqSYZcA',2),(108,'2025-12-30 22:48:25.698373','2026-01-06 22:48:25.698373',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzEwOTcwNSwiZXhwIjoxNzY3NzE0NTA1fQ._Lvn4GYjU6kjgArXf1AIDMIBmv2kkPtfsjn14af8Su8Bp9b3DfYAy6Zwzut2IsyCSOS58U0ESmpxuAPBZOWDfA',2),(109,'2025-12-31 10:03:38.360978','2026-01-07 10:03:38.360978',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE1MDIxOCwiZXhwIjoxNzY3NzU1MDE4fQ.pnwgY-z4WS1SK9ceB1vvjaStzooK17ICCiOTrjUf8vlK0q04O4Tr9JecAHLbR9NJ6oKsZ9J3wgB7pkltt7DtJA',2),(110,'2025-12-31 10:10:27.410124','2026-01-07 10:10:27.410124',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTA2MjcsImV4cCI6MTc2Nzc1NTQyN30.gMxdyocUb2M0Em5tdE8Fftt5_y4bcnjCya03SUh_R0AitrMo8Vpb4OVFJAttGlkMYvGriVvpbd-QyO134VKBXA',1),(111,'2025-12-31 11:33:58.703437','2026-01-07 11:33:58.703437',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE1NTYzOCwiZXhwIjoxNzY3NzYwNDM4fQ.AmMb3GHWEf7rvNM3EtLhqVd46FAjw5rVspwuTu-GiCA7EBPZsYPVv5zKKiDbTMwuNy561X7P_WUFhvVBf-Qr8Q',2),(112,'2025-12-31 11:34:18.948104','2026-01-07 11:34:18.948104',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTU2NTgsImV4cCI6MTc2Nzc2MDQ1OH0.wt-g9sgdr-SWLn_MkjkscoY_KuuVf6IYD8dOFzoe-Vv4NU9NVnQPa444RyNogZMWcSQJLxHM_h9WNyXS0JleYg',1),(113,'2025-12-31 11:51:11.023162','2026-01-07 11:51:11.023162',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTY2NzAsImV4cCI6MTc2Nzc2MTQ3MH0.ujr_W6Ir3yOh1tAFri0t8oxAui6eVZDN5vpkEVarktrm-pSWgoh8RnsdDvsFfTLKbCd7Ru5zzomn07koK1WNbw',1),(114,'2025-12-31 11:55:48.237897','2026-01-07 11:55:48.237897',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE1Njk0OCwiZXhwIjoxNzY3NzYxNzQ4fQ.kpKILwPWWL9V1reKdNUKfaK7A4Z-CrCkalrQDpYJCgqSk4UtQrsQH9MwmS1TowDZ8aVUFRFSWmlxm0IyeEeZMg',2),(115,'2025-12-31 12:06:34.376974','2026-01-07 12:06:34.376974',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTc1OTQsImV4cCI6MTc2Nzc2MjM5NH0.esaOLod6QdYyCjiczSc5CpWuCMI0RPsGqdM4lyz1-b8rfPOflsxaqgKAx164xun1us_sW1mJNojIPQGtjso6tA',1),(116,'2025-12-31 12:33:21.900095','2026-01-07 12:33:21.900095',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE1OTIwMSwiZXhwIjoxNzY3NzY0MDAxfQ.G5-bgfcxjjGbr21ovBSt8atAVqg8RVl7sTQNXLie5sq21suRlOLVeqNoU4g0guVNlLVSJO2ukrBRL3OJ73Ka5Q',2),(117,'2025-12-31 12:45:07.980425','2026-01-07 12:45:07.980425',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTk5MDcsImV4cCI6MTc2Nzc2NDcwN30.JedDbi80FLe0cEmsZT1tUsm5c1MF1jlQKesVtQM-_mbIrzxFwe4t8A3naH4mNkDD7g18c_TlVJK2kDvP8oCWyQ',1),(118,'2025-12-31 12:45:57.895275','2026-01-07 12:45:57.895275',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE1OTk1NywiZXhwIjoxNzY3NzY0NzU3fQ.kYsRi4V_gNrcBrLmTeIm3fXs3Vo-rsThwKFs1aZch5WFv18SotoIFZqVnjLTGlRXe0Uy6UNJQt2AtGFp-8UvXQ',2),(119,'2025-12-31 12:46:32.304276','2026-01-07 12:46:32.304276',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNTk5OTIsImV4cCI6MTc2Nzc2NDc5Mn0.K6E1zvVbiEw7sWTW8yZ7p2Qq8RuQde7_bK6_xVsCAjtD7j8Txj7i6bAp8XXDLgvzIWQqKuVfhQEOor2tDos5iQ',1),(120,'2025-12-31 12:46:58.500078','2026-01-07 12:46:58.500078',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE2MDAxOCwiZXhwIjoxNzY3NzY0ODE4fQ.wKlBeN3xpOstBCJjplLNJ9IQXQt3o_VvfV3PnIg-6CCDKezLZAfv1nJUaRqfruekrZ6LgqVvSek0q_deK6pZYw',2),(121,'2025-12-31 13:39:28.171689','2026-01-07 13:39:28.171689',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE2MzE2OCwiZXhwIjoxNzY3NzY3OTY4fQ.e4T1bx8pwXnspC6ev0DiwGOwxGxrUfhy_OAK4O5_MP0JmTX5tC2Ojf-ldnYtHvpLux3_59AtnzulRep0FfDcAA',2),(122,'2025-12-31 13:44:47.378649','2026-01-07 13:44:47.378649',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNjM0ODcsImV4cCI6MTc2Nzc2ODI4N30.tvbxwkmDF6hENNyOwlqkL4g_KtmRvl2np5xYVHeMmbWzFImhU3omievhfrsOuUOgMFZrUInnhkBaK8q_Mt-5Dg',1),(123,'2025-12-31 13:45:37.316675','2026-01-07 13:45:37.316675',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE2MzUzNywiZXhwIjoxNzY3NzY4MzM3fQ.P8ea4bi_zO8AKy9LRDcSYnRMsoZJD0TvYfFtGGV59RcYvXsDwrNi6YgyIRe81PJ0R6H2tJ5JeVsyyavTqhTyeA',2),(124,'2025-12-31 13:46:30.529831','2026-01-07 13:46:30.529831',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6MSwic3ViIjoiYWRtaW5AbG9jYWwiLCJpYXQiOjE3NjcxNjM1OTAsImV4cCI6MTc2Nzc2ODM5MH0.1BV478lvM4aUvPYwm6oFVqH2jNpG0c5CTLZ0o25hYvmGIUmTEtds_XCyB7b7-cSA0mN0IpR4HJnq1tVyC7E91g',1),(125,'2025-12-31 17:46:32.555366','2026-01-07 17:46:32.555366',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE3Nzk5MiwiZXhwIjoxNzY3NzgyNzkyfQ.36ce0ruihi-fb7RK85FADLY74Jusz7IIncOPiSCT_frXbjqNoj2CfbuC5sMGpB5FRLk73qBkYmPJJYY5bGlQtw',2),(126,'2025-12-31 20:17:52.435366','2026-01-07 20:17:52.435366',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE4NzA3MiwiZXhwIjoxNzY3NzkxODcyfQ.O0HyEHHWhQ_ZP5HVDWQKOtYHX7zBiFn_htnvkcBzYLxPr0jMFGtaULP5cXqVJqpxKtGnSqWA62Mwp2jzx74FmA',2),(127,'2025-12-31 23:44:31.939856','2026-01-07 23:44:31.939856',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE5OTQ3MSwiZXhwIjoxNzY3ODA0MjcxfQ.xDFAs-PWit8CTX9Yf8UihA2w2kt3WTUB6sssHp8dRQavBA6_mBUtaOxswIV9s0Wpglg0cS1Ll74C_kl073LzPA',2),(128,'2025-12-31 23:48:20.629275','2026-01-07 23:48:20.629275',_binary '','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzE5OTcwMCwiZXhwIjoxNzY3ODA0NTAwfQ.hdpRE3FFrq2oGUJKHL3vH4o0pZhu5hCto_QF8_n4hdUhtOdNwWMstWwJlEbz9165ddyF0zj6pJYIk0gbP68PSA',2),(129,'2026-01-01 09:35:47.882329','2026-01-08 09:35:47.881332',_binary '\0','eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJ0eXBlIjoiUkVGUkVTSCIsInVzZXJJZCI6Miwic3ViIjoidmluaC50aGFuaG5ndXllbjI2QGdtYWlsLmNvbSIsImlhdCI6MTc2NzIzNDk0NywiZXhwIjoxNzY3ODM5NzQ3fQ.Tnq0xZmTJHH3gbU_Rngk-H_yuKohZ2Esv07BMODzmQDPGWSqwTcYsw-o54C0BtwGmc2EJa1dV9EZsPHA4-88QA',2);
/*!40000 ALTER TABLE `refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shipping_orders`
--

DROP TABLE IF EXISTS `shipping_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shipping_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `carrier` varchar(255) DEFAULT NULL,
  `cod_amount` decimal(38,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `delivery_type` varchar(255) DEFAULT NULL,
  `distance_km` double DEFAULT NULL,
  `estimated_delivery_time` varchar(255) DEFAULT NULL,
  `shipping_fee` decimal(38,2) DEFAULT NULL,
  `status` enum('CANCELLED','CONFIRMED','DELIVERED','PENDING','RETURNED','SHIPPING') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9nemmy106wav6coahay0rywhl` (`order_id`),
  CONSTRAINT `FKlfb3u79ee02ok91hxcwqp4ji1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shipping_orders`
--

LOCK TABLES `shipping_orders` WRITE;
/*!40000 ALTER TABLE `shipping_orders` DISABLE KEYS */;
INSERT INTO `shipping_orders` VALUES (1,'Tulip Shipping',90000.00,'2025-12-28 20:25:12.178791',NULL,'STANDARD',NULL,NULL,46000.00,'SHIPPING','2025-12-28 20:26:09.371724',24),(2,'Tulip Shipping',0.00,'2025-12-28 21:07:35.515144',NULL,'STANDARD',NULL,NULL,15000.00,'CONFIRMED','2025-12-28 21:10:40.772476',25),(3,'Tulip Shipping',706000.00,'2025-12-28 21:16:19.812435',NULL,'STANDARD',NULL,NULL,46000.00,'CONFIRMED','2025-12-30 11:40:21.829717',26),(4,'Tulip Shipping',136000.00,'2025-12-28 21:45:51.747567',NULL,'STANDARD',NULL,NULL,46000.00,'SHIPPING','2025-12-28 21:47:31.958757',27),(5,'Tulip Shipping',136000.00,'2025-12-28 22:12:29.504553',NULL,'STANDARD',NULL,NULL,46000.00,'SHIPPING','2025-12-30 13:43:53.092517',28),(6,'Tulip Shipping',0.00,'2025-12-28 22:15:47.923867',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-28 22:18:17.610881',29),(7,'Tulip Shipping',120000.00,'2025-12-29 10:43:44.570491',NULL,'FAST',NULL,NULL,30000.00,'SHIPPING','2025-12-29 10:48:16.042088',30),(8,'Tulip Shipping',0.00,'2025-12-29 11:05:01.815892',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-29 11:07:50.562735',31),(9,'Tulip Shipping',1815000.00,'2025-12-29 11:15:27.576555',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-29 11:16:46.034617',32),(10,'Tulip Shipping',900000.00,'2025-12-30 11:02:31.696055',NULL,'STANDARD',NULL,NULL,30000.00,'CONFIRMED','2025-12-30 11:37:57.622032',33),(11,'Tulip Shipping',370200.00,'2025-12-30 17:57:09.719208',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-30 18:08:15.396151',34),(12,'Tulip Shipping',675000.00,'2025-12-30 21:37:19.419730',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-30 21:40:03.881437',35),(13,'Tulip Shipping',195000.00,'2025-12-31 12:43:09.841322',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-31 12:45:23.798425',36),(14,'Tulip Shipping',59000.00,'2025-12-31 13:41:21.611737',NULL,'STANDARD',NULL,NULL,15000.00,'SHIPPING','2025-12-31 13:45:26.963126',37);
/*!40000 ALTER TABLE `shipping_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sizes`
--

DROP TABLE IF EXISTS `sizes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sizes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sort_order` int DEFAULT NULL,
  `code` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sizes`
--

LOCK TABLES `sizes` WRITE;
/*!40000 ALTER TABLE `sizes` DISABLE KEYS */;
INSERT INTO `sizes` VALUES (1,1,'S'),(2,2,'M'),(3,3,'L'),(4,4,'XL');
/*!40000 ALTER TABLE `sizes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_history`
--

DROP TABLE IF EXISTS `stock_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint NOT NULL,
  `previous_quantity` int DEFAULT NULL,
  `new_quantity` int DEFAULT NULL,
  `change_amount` int DEFAULT NULL,
  `admin_username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stock_id` (`stock_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `stock_history_ibfk_1` FOREIGN KEY (`stock_id`) REFERENCES `product_stock` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_history`
--

LOCK TABLES `stock_history` WRITE;
/*!40000 ALTER TABLE `stock_history` DISABLE KEYS */;
INSERT INTO `stock_history` VALUES (1,1,23,23,0,'admin','Manual update from inventory page','2025-12-25 08:25:16'),(2,2,31,31,0,'admin','Manual update from inventory page','2025-12-25 08:25:20'),(3,1,23,24,1,'admin','Manual update from inventory page','2025-12-25 08:25:26'),(4,1,24,24,0,'admin','Manual update from inventory page','2025-12-25 08:25:34'),(5,1,24,24,0,'admin','Manual update from inventory page','2025-12-25 08:25:38'),(6,1,24,24,0,'admin','Manual update from inventory page','2025-12-25 08:25:43'),(7,17,0,2,2,'admin','Manual update from inventory page','2025-12-25 08:26:11'),(8,1,24,24,0,'admin','Manual update from inventory page','2025-12-25 08:32:21'),(9,2,31,31,0,'admin','Manual update from inventory page','2025-12-25 08:32:22'),(10,17,1,1,0,'admin','Manual update from inventory page','2025-12-25 08:32:33'),(11,17,1,1,0,'admin','Manual update from inventory page','2025-12-25 08:33:54'),(12,17,2,2,0,'admin','Manual update from inventory page','2025-12-25 11:35:22'),(13,17,2,2,0,'admin','Manual update from inventory page','2025-12-25 12:20:35'),(14,29,1,36,35,'admin','Manual update from inventory page','2025-12-25 13:30:05'),(15,28,17,17,0,'admin','Manual update from inventory page','2025-12-28 14:13:34'),(16,17,2,1,-1,'admin','Manual update from inventory page','2025-12-31 06:48:05');
/*!40000 ALTER TABLE `stock_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_addresses`
--

DROP TABLE IF EXISTS `user_addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_addresses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address_line` varchar(512) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `is_default` bit(1) DEFAULT NULL,
  `province` varchar(100) DEFAULT NULL,
  `recipient_name` varchar(255) NOT NULL,
  `recipient_phone` varchar(50) NOT NULL,
  `village` varchar(100) DEFAULT NULL,
  `profile_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKh9r98weiqb224xhtw9fw918td` (`profile_id`),
  CONSTRAINT `FKh9r98weiqb224xhtw9fw918td` FOREIGN KEY (`profile_id`) REFERENCES `user_profiles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_addresses`
--

LOCK TABLES `user_addresses` WRITE;
/*!40000 ALTER TABLE `user_addresses` DISABLE KEYS */;
INSERT INTO `user_addresses` VALUES (1,'Đại học bách khoa hà nội','Quận Hai Bà Trưng',_binary '','Thành phố Hà Nội','Tèo Văn Tí','0907384925','Phường Bạch Mai',1),(2,' The Sun Avenue','Thành phố Thủ Đức',_binary '','Thành phố Hồ Chí Minh','Thành Vinh','0907384922','Phường Bình Thọ',2),(3,'1 vvn','Huyện Đại Từ',_binary '','Tỉnh Thái Nguyên','Trùm Bom Hàng','0906661587','Xã Hoàng Nông',3);
/*!40000 ALTER TABLE `user_addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profiles`
--

DROP TABLE IF EXISTS `user_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `birthday` date DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `address` varchar(512) DEFAULT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe5h89rk3ijvdmaiig4srogdc6` (`user_id`),
  CONSTRAINT `FKjcad5nfve11khsnpwj1mv8frj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_profiles`
--

LOCK TABLES `user_profiles` WRITE;
/*!40000 ALTER TABLE `user_profiles` DISABLE KEYS */;
INSERT INTO `user_profiles` VALUES (NULL,NULL,'2025-11-29 19:43:38.342616',1,'2025-11-29 19:43:38.342616',1,NULL,NULL,NULL,'Administrator'),(NULL,NULL,'2025-12-02 14:17:22.546186',2,'2025-12-02 14:17:22.546186',2,NULL,NULL,'https://lh3.googleusercontent.com/a/ACg8ocKp8m57JCoSHVn7wnJAtUtlnH_Qs6BbN0FzyOm13UCISJFdNHvP=s96-c','Thành Vinh'),(NULL,NULL,'2025-12-02 14:18:07.267923',3,'2025-12-02 14:18:07.267923',3,NULL,NULL,'https://lh3.googleusercontent.com/a/ACg8ocIOrfLrNEwf4_x_pqty5hOJVAZ0Lkfw-w1XX7VAJtFnmXb2Qw=s96-c','Nguyen Thanh Vinh'),(NULL,NULL,'2025-12-17 13:55:07.443061',4,'2025-12-30 22:27:05.015241',4,'0773764793',NULL,'https://lh3.googleusercontent.com/a/ACg8ocJclLZkrpfW4AiFHZc8nacVSieeROi1goCKdgv7fvhnPQsKng=s96-c','Vinh zx'),(NULL,NULL,'2025-12-30 22:23:38.694864',5,'2025-12-30 22:23:38.694864',5,'0773764799',NULL,NULL,'NGUYEN THANH VINH');
/*!40000 ALTER TABLE `user_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `status` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','CUSTOMER') NOT NULL,
  `auth_provider` varchar(50) DEFAULT NULL,
  `email_verified_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (_binary '','2025-11-29 19:43:38.317574',1,'2025-11-29 19:43:38.317574','admin@local','$2a$10$FGLGU6l8MGH321n60Pbi/.O1sKH/0Er0fGqd2MC9TJMLuvjl3EDCa','ADMIN','LOCAL','2025-12-17 13:54:08.116693'),(_binary '','2025-12-02 14:17:22.506198',2,'2025-12-17 13:54:08.140170','vinh.thanhnguyen26@gmail.com',NULL,'CUSTOMER','GOOGLE','2025-12-17 13:54:08.116693'),(_binary '','2025-12-02 14:18:07.259921',3,'2025-12-27 20:46:32.316349','23110172@student.hcmute.edu.vn',NULL,'CUSTOMER','GOOGLE','2025-12-27 20:46:32.307443'),(_binary '','2025-12-17 13:55:07.440096',4,'2025-12-30 22:27:05.026107','concacc557@gmail.com','$2a$10$LzGLfcyKggY9zVzwbCvW/u./aUGkAOC9CDTnYLVXGrDBocrpYUiO2','CUSTOMER','GOOGLE','2025-12-17 13:55:31.365235'),(_binary '','2025-12-30 22:23:38.641623',5,'2025-12-30 22:23:38.641623','concacc555@gmail.com','$2a$10$wbHnplwzM0obuss6FdZ9q.uxuEPJ7LIzmp1VuoGBkErNIUi9gLv4a','CUSTOMER','LOCAL',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vouchers`
--

DROP TABLE IF EXISTS `vouchers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vouchers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `discount_value` decimal(38,2) DEFAULT NULL,
  `expire_at` datetime(6) DEFAULT NULL,
  `min_order_value` decimal(38,2) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `status` bit(1) DEFAULT NULL,
  `type` enum('AMOUNT','PERCENT') DEFAULT NULL,
  `used_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK30ftp2biebbvpik8e49wlmady` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vouchers`
--

LOCK TABLES `vouchers` WRITE;
/*!40000 ALTER TABLE `vouchers` DISABLE KEYS */;
/*!40000 ALTER TABLE `vouchers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishlist_items`
--

DROP TABLE IF EXISTS `wishlist_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlist_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtp53unkks741xiqi6m620i7mx` (`user_id`,`product_id`),
  KEY `FKqxj7lncd242b59fb78rqegyxj` (`product_id`),
  CONSTRAINT `FKmmj2k1i459yu449k3h1vx5abp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqxj7lncd242b59fb78rqegyxj` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlist_items`
--

LOCK TABLES `wishlist_items` WRITE;
/*!40000 ALTER TABLE `wishlist_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `wishlist_items` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-01  9:42:34
