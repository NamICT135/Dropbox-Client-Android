# Trạng thái MVP Dropbox (29/10/2026)

## Quyết định phiên và quyền truy cập

Các tài khoản **cục bộ trên cùng thiết bị dùng chung một kết nối Dropbox App Folder**.
Local Logout chỉ kết thúc phiên cục bộ và **giữ token Dropbox**; Account > Disconnect
xóa token Dropbox trên thiết bị. Không đưa email/mật khẩu Dropbox vào ứng dụng.
Checklist tham chiếu có mục “đăng xuất và xóa credential cục bộ” và “hỗ trợ nhiều
tài khoản”. Nếu diễn giải là cô lập credential cho từng tài khoản cục bộ thì mục đó
**không phù hợp với quyết định sản phẩm hiện tại**. Chủ sản phẩm phải đổi quyết định
trước khi triển khai cách ly từng người dùng.

## Bằng chứng và giới hạn

| Nhóm checklist | Trạng thái | Bằng chứng / phần chưa xác nhận |
| --- | --- | --- |
| OAuth PKCE, credential mã hóa, kết nối dùng chung | Implemented; JVM test cho logic phiên | Chưa xác nhận luồng OAuth mới trên thiết bị trong lần sửa này. |
| Duyệt, phân trang, ID merge, lịch sử Back | Implemented; JVM test merge | Chưa chạy UI nhiều trang / Back trên thiết bị. Xoay màn hình màn transfer đã qua emulator test. |
| Upload, download, create, rename, delete | Implemented; JVM test SDK giả và helper; emulator test I/O | Tải xuống dùng file tạm, kiểm tra size và Dropbox content hash nếu có. Android không đảm bảo ghi đè atomic cho mọi DocumentsProvider; nếu ghi URI đích thất bại, báo tài liệu có thể bị ghi một phần và không xóa URI. Chưa chạy thử nhiều loại DocumentsProvider thật trong lần sửa này. |
| Nhóm lỗi và response cũ | Implemented; JVM test một số mapping và revision phiên | Mất mạng, token, thiếu scope, not-found, rate limit và cloud full có hướng xử lý. Chưa kiểm tra mọi mã lỗi SDK/live. |
| Accessibility / locale | Implemented một phần | Menu item 48dp, nhãn TalkBack, live region và tài nguyên tiếng Việt. Chưa audit TalkBack, tương phản, màn hình nhỏ/landscape trên thiết bị. |
| CI | Implemented | Workflow chạy assembleDebug, testDebugUnitTest, lintDebug. Chưa có bằng chứng một lần chạy GitHub Actions. |

Lần kiểm tra này: `bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline`
thành công (47 JVM tests, lint 0 lỗi). `:app:assembleDebugAndroidTest` thành công.
Trên emulator `emulator-5554`, chạy trực tiếp từng class bằng `am instrument`:
`DropboxTransferServiceIoTest` 5/5, `TransferFlowTest` 4/4,
`SessionManagerTest` 4/4. Các test này dùng fake HTTP hoặc phiên riêng,
không xác nhận Dropbox thật. Không có `DROPBOX_APP_KEY` trong local.properties
của workspace nên không chạy live Dropbox.

## Transfer sau process death

Transfer hiện thuộc `TransferViewModel` và executor của tiến trình; xoay màn hình
giữ ViewModel nhưng **process death dừng transfer**. File tạm tải xuống còn trong
cache nếu tiến trình chết đột ngột và sẽ được Android dọn theo chính sách cache.
Không quảng bá transfer bền
vững hoặc progress/cancel/retry hiện chưa có. Nếu cần tải nền bền vững sau MVP,
thiết kế một `WorkManager` worker riêng: lưu URI đọc được lâu dài bằng SAF
persistable permission, lưu trạng thái theo ID, dùng foreground notification cho
transfer dài, tiến trình dựa trên byte thực, cancel có cleanup, retry hữu hạn theo
loại lỗi và idempotency của Dropbox. Download cần staging và xác minh trước khi
ghi URI đích; upload cần tránh retry mù khi kết quả commit chưa chắc chắn.
Không chuyển MVP sang WorkManager khi chưa có test thiết bị cho process death.

## Release và bài nộp

`release` hiện không cấu hình signing và tắt optimization; build release mặc định
không phù hợp để phát hành. Trước khi phát hành: tạo keystore riêng ngoài repo,
lưu mật khẩu và đường dẫn trong secret store của CI, đọc qua environment
variables ở cấu hình release, kiểm tra signing bằng `apksigner verify` và giữ
keystore/backup an toàn. Không commit keystore, mật khẩu, `local.properties` hay token.
Chưa có LICENSE. **Chủ sở hữu repo cần quyết định loại giấy phép** trước khi nộp
công khai; không tự chọn một license thay họ.

## Việc thủ công trước demo

1. Cấu hình public `DROPBOX_APP_KEY`, scope tối thiểu và tài khoản Dropbox thử
   nghiệm; chuẩn bị folder/tệp mẫu không quan trọng.
2. Trên thiết bị thật: kết nối OAuth, đổi tài khoản Dropbox, duyệt nhiều trang,
   quay Back, upload, download PDF/JPG/PNG và so khớp byte/hash, rename/delete,
   thử mất mạng, thiếu scope, token bị thu hồi, hết dung lượng và xoay màn hình.
3. Kiểm tra TalkBack, thứ tự focus, màn hình nhỏ và landscape. Nếu có kết nối
   Dropbox đã lưu trên thiết bị, dùng `adb install -r` và chạy từng test live;
   không dùng `connectedAndroidTest` vì nó có thể gỡ app và xóa phiên.
