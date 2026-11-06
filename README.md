# Moments4j

[moments](https://github.com/greyli/moments)(Flask) 的 Java 1:1 复刻版 — 一个照片分享社交网络。

A Java port of the Flask photo-sharing app [Moments](https://github.com/greyli/moments), 1:1 in features, routes, templates and package layout.

![demo](https://github.com/greyli/moments/raw/main/demo.png)

## 技术栈对照 / Stack mapping

| Python (moments) | Java (moments4j) |
| --- | --- |
| Flask | Spring Boot 3 (Web MVC) |
| Jinja2 | Thymeleaf |
| Flask-SQLAlchemy | Spring Data JPA (Hibernate + SQLite) |
| Flask-Login | Spring Security (session auth + remember-me) |
| Flask-WTF (CSRF) | Spring Security CSRF |
| Flask-Mail | Spring Boot Mail (JavaMailSender, async) |
| Flask-Dropzone | Dropzone.js (静态 JS + 等价配置) |
| Flask-Avatars (Identicon) | moments4j AvatarService (BufferedImage 绘制 + 裁剪) |
| Flask-Bootstrap | Bootstrap 5 静态资源 + bootstrap-icons CDN |
| Flask-Whooshee 全文搜索 | JPA LIKE 查询 (见下「简化说明」) |
| Pillow | Thumbnailator |
| PyJWT | jjwt (HS256, 同样的 payload 结构) |
| Faker | datafaker |
| Flask CLI (init-db/init-app/lorem) | `java -jar moments4j.jar <command>` |

## 包结构对照 / Package layout

```
moments/                        moments4j/src/main/java/moments4j/
├── app.py                      Moments4jApplication.java
├── moments/
│   ├── __init__.py             (Spring 自动装配, 无对应文件)
│   ├── settings.py             settings/MomentsSettings.java
│   ├── models.py               model/ (User, Role, Permission, Photo, Tag,
│   │                                     Comment, Follow, Collection, Notification)
│   ├── decorators.py           core/decorators/ (@LoginRequired, @ConfirmRequired,
│   │                                     @PermissionRequired) + core/security/AuthInterceptor
│   ├── utils.py                core/Utils.java
│   ├── emails.py               service/MailService.java
│   ├── notifications.py        service/NotificationService.java
│   ├── lorem.py                core/Lorem.java
│   ├── core/
│   │   ├── commands.py         core/Commands.java
│   │   ├── errors.py           core/Errors.java (@ControllerAdvice)
│   │   ├── extensions.py       core/security/SecurityConfig.java
│   │   ├── logging.py          logback-spring.xml
│   │   ├── request.py          (慢查询日志, 见简化说明)
│   │   └── templating.py       core/TemplatingAdvice.java
│   ├── blueprints/             blueprints/
│   │   ├── main.py             MainController.java
│   │   ├── auth.py             AuthController.java
│   │   ├── user.py             UserController.java
│   │   ├── admin.py            AdminController.java
│   │   └── ajax.py             AjaxController.java
│   ├── forms/                  forms/ (Login/Register/EditProfile/... Form)
│   ├── templates/              src/main/resources/templates (逐文件对应)
│   └── static/                 src/main/resources/static (原样复制)
└── tests/                      src/test/java/moments4j/ (test_auth/test_main/test_user/
                                          test_admin/test_ajax/test_cli/test_basic 对应)
```

## 运行 / Run

要求: JDK 17+, Maven 3.8+

```bash
# 构建并测试
mvn package

# 初始化数据库 + 角色权限
java -jar target/moments4j-1.0.0.jar init-app

# 生成假数据 (可选)
java -jar target/moments4j-1.0.0.jar lorem --user=10 --photo=30 --comment=100

# 启动
java -jar target/moments4j-1.0.0.jar
# 打开 http://localhost:8080
```

管理员账号: 注册 email 为 `admin@helloflask.com` (可用环境变量 `MOMENTS_ADMIN` 覆盖) 的用户即为 Administrator。lorem 生成的默认管理员是 `greyli / moments`。

## 配置

| 环境变量 | 说明 | 默认 |
| --- | --- | --- |
| `MOMENTS_ADMIN` | 管理员邮箱 | `admin@helloflask.com` |
| `MOMENTS_UPLOAD_PATH` | 上传目录 | `uploads` |
| `SECRET_KEY` | JWT/session 密钥 | `secret string` |
| `MAIL_SERVER` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 配置, 未配置则跳过发信 | 空 |
| `MOMENTS_LOGGING_PATH` | 日志文件 | `logs/moments.log` |

## 测试

```bash
mvn test   # 71 tests, 对应原项目 tests/test_*.py (test_ui 的 Selenium 用例未移植)
```

## 简化说明 (ponytail: 注释标记处)

- **搜索**: whooshee 全文索引用 JPA `LIKE %q%` 代替; 数据量大或需要分词时再引入 Hibernate Search/Lucene。
- **错误邮件**: logging.py 的 SMTPHandler(500 错误发邮件给管理员)未实现, logback 文件滚动已就位, 需要时在 logback-spring.xml 加 SMTPAppender。
- **慢查询日志**: core/request.py 的 SQLAlchemy 查询计时未移植, 可用 p6spy 或 Hibernate statistics 替代。
- **test_ui**: Selenium UI 测试未移植。

## License

MIT (与原项目一致)
