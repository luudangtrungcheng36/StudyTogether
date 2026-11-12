# Chạy thử Discord study

Luồng hiện tại: JDA event → `DiscordVoiceListener` → command record → `StudySessionService` → PostgreSQL. Listener bỏ qua bot ở cả ba callback. Chỉ phòng có `tracking_enabled = true` trong `study_rooms` được bắt đầu tính giờ.

## Chạy kiểm tra ngay, không cần token Discord

Từ thư mục gốc dự án, dùng Java 25, Maven và PostgreSQL đã cài:

```powershell
.\scripts\Test-DiscordStudy.ps1
```

Nếu PostgreSQL nằm ở nơi khác:

```powershell
.\scripts\Test-DiscordStudy.ps1 -PostgresBin 'D:\PostgreSQL\bin'
```

Script tạo một PostgreSQL tạm trong `target/discord-study-pg-*`, chỉ lắng nghe ở `127.0.0.1`, áp dụng SQL draft trong test và chạy các kiểm tra liên quan. Không dùng database mặc định hoặc kết nối Discord. PostgreSQL tạm được dừng ở cuối; file và báo cáo còn lại trong `target/`. Các biến `TEST_DB_*` trước đó được khôi phục.

Test listener dùng các lớp event JDA thật với member/voice state mô phỏng, rồi kiểm tra dữ liệu PostgreSQL thật qua service Spring có transaction. Kịch bản chính: vào A → bật camera → bật chia sẻ → tắt camera → tắt chia sẻ → chuyển B với camera/chia sẻ đang bật → rời voice. Kết quả mong đợi: một user, một identity, hai session đã đóng và bốn segment đã đóng. Segment chỉ ghi các khoảng có camera hoặc chia sẻ màn hình; khoảng voice-only không tạo segment, còn tổng thời gian học được tính từ session. Có thêm kiểm tra bỏ qua bot, phòng lạ/phòng tắt tracking, media ngoài voice và rollback.

## Chạy với bot Discord thật

1. Chuẩn bị database demo PostgreSQL **mới**, rồi áp dụng thủ công [SQL draft](./sql/schema.sql) vào database đó. Không chạy script tạo bảng lên database đang có bảng/dữ liệu cần giữ. Bước này không chốt SQL thành V1; Flyway vẫn tắt, Hibernate vẫn `none`, SQL init vẫn `never`.
2. Cấu hình biến môi trường trong terminal/IDE chạy ứng dụng: `DB_USERNAME`, `DB_PASSWORD`, `DISCORD_BOT_TOKEN`. Nếu dùng database demo khác URL mặc định, thêm `SPRING_DATASOURCE_URL`, ví dụ `jdbc:postgresql://localhost:5432/study_community_demo`. Giữ token ngoài Git và chat.
3. Bot cần được thêm vào server và nhìn thấy phòng voice muốn theo dõi. Bật Developer Mode trong Discord để Copy Server ID và Copy Channel ID.
4. Trong database demo, đăng ký phòng bằng SQL dưới đây, thay hai giá trị ID bằng ID thật:

```sql
INSERT INTO study_rooms (discord_guild_id, discord_channel_id, display_name)
VALUES ('GUILD_ID', 'CHANNEL_ID', 'Study room A');
```

Đăng ký thêm phòng B nếu muốn thử chuyển giữa hai phòng được theo dõi. Nếu dòng đã tồn tại, kiểm tra `tracking_enabled` thay vì chèn trùng.

Chạy ứng dụng trong terminal đã có các biến môi trường:

```powershell
mvn spring-boot:run
```

Sau khi bot kết nối, dùng tài khoản người vào phòng A, bật/tắt camera và chia sẻ, chuyển B rồi rời voice. Nếu đã ở trong phòng trước khi bot khởi động, hãy rời rồi vào lại để tạo sự kiện join mới; đồng bộ người đang ở sẵn/recovery chưa được triển khai.

Log `Voice update processed` xuất hiện sau khi service xử lý thành công. `startedSessionId` có UUID nếu tạo session mới; `null` cũng có thể là rời phòng, phòng không được theo dõi hoặc một session khác đang mở. Đây không phải thông báo rằng mọi lần vào phòng đều được ghi nhận. Muốn xem log camera/chia sẻ, cấu hình `LOGGING_LEVEL_COM_STUDYCOMMUNITY_DISCORD_LISTENER=DEBUG` cho phiên demo.

Sau khi rời voice, xem kết quả trong database demo:

```sql
SELECT i.provider_subject AS discord_user_id,
       r.discord_channel_id,
       s.started_at, s.ended_at, s.end_reason,
       EXTRACT(EPOCH FROM s.ended_at - s.started_at) AS duration_seconds
FROM study_sessions s
JOIN user_identities i ON i.id = s.identity_id
JOIN study_rooms r ON r.id = s.study_room_id
ORDER BY s.started_at;

SELECT r.discord_channel_id, g.started_at, g.ended_at,
       g.camera_on, g.screen_share_on,
       EXTRACT(EPOCH FROM g.ended_at - g.started_at) AS duration_seconds
FROM study_session_segments g
JOIN study_sessions s ON s.id = g.study_session_id
JOIN study_rooms r ON r.id = s.study_room_id
ORDER BY s.started_at, g.started_at;
```

`observedAt` do backend lấy khi xử lý callback, không phải thời điểm thao tác do Discord gửi. Thời gian của session đang mở chưa phải tổng thời gian cuối cùng. API lịch sử/thống kê, khôi phục sau mất kết nối và xử lý sự kiện đồng thời vẫn chưa nằm trong luồng demo này.
