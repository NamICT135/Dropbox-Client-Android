# Chạy và trình bày Dropbox Client

Lời thoại ngắn, thứ tự thao tác và câu hỏi thường gặp: [Kịch bản trình bày 5–7 phút](presentation-script-vi.md).

## Chuẩn bị một lần

1. Trong Android Studio, chọn **File > Open** và mở thư mục gốc của repository (thư mục có `settings.gradle.kts`). Đợi Gradle Sync hoàn tất.
2. Trong Android Studio, chọn emulator hoặc điện thoại Android rồi chạy cấu hình **app**. Nếu máy chưa có SDK, cài Android SDK 36.1; project dùng JBR 21 đi kèm Android Studio.
3. Để gọi API thật, mở `local.properties` ở gốc project, giữ dòng `sdk.dir` của máy và thêm `DROPBOX_APP_KEY=...` bằng App key từ Dropbox App Console. Không đưa file này, mật khẩu, App secret hoặc token lên Git.
4. Trong Dropbox App Console, kiểm tra App Folder và bốn quyền: `account_info.read`, `files.metadata.read`, `files.content.read`, `files.content.write`. Nếu vừa đổi quyền, lưu và kết nối lại trong app.

Không có App key, phần tài khoản cục bộ và unit tests vẫn chạy, nhưng app không thể kết nối Dropbox. Thiết bị mới phải tự hoàn thành OAuth; không thể sao chép phiên từ Git.

## Trình tự chạy trên điện thoại

1. Mở **Dropbox Client**. Chọn **Create an account**, nhập username 3–24 ký tự và password 8–128 ký tự. Các ô tên và email có thể để trống; không cần email cá nhân.
2. Sau khi vào **Account**, chọn **Set up project storage > Connect project Dropbox**. Trên trang cấp quyền Dropbox, kiểm tra tài khoản đang đăng nhập đúng là tài khoản nhóm rồi bấm **Allow**. Account trong app chỉ hiện **Connected to Dropbox**, không hiện tên hoặc email Dropbox. Nếu trình duyệt đang dùng tài khoản khác, chọn **Disconnect project storage** trong app, đăng xuất tài khoản đó trong trình duyệt, đăng nhập tài khoản nhóm rồi kết nối lại.
3. Chạm **Files** ở thanh dưới. App tải thư mục gốc của App Folder. Dùng **Refresh** để tải lại, chạm folder để mở, **Up** để quay về folder cha và **Load more** nếu còn trang.
4. Chọn **Create folder**, nhập tên và xác nhận. Dùng nút menu cạnh folder để **Rename** hoặc **Delete**; Delete có hộp thoại xác nhận.
5. Đứng trong folder cần tải lên, chạm **Upload file**, chọn một file nhỏ bằng Android document picker. App hiển thị tên và đường dẫn Dropbox thực tế; quay lại Files để xem danh sách đã refresh.
6. Mở **Search**, nhập một phần tên file/folder và bấm **Search Dropbox**. Chạm folder kết quả để mở trong Files; chạm file để đến màn hình tải xuống hoặc xem trước. Có thể bấm **Load more** khi Dropbox còn kết quả.
7. Chạm file ảnh/PDF trong Files rồi chọn **Preview** để mở bằng viewer Android. Với file khác, chọn **Download from Dropbox**, chọn nơi lưu bằng Android picker. Đối chiếu file đã tải với bản gốc. Preview giới hạn 20 MB và cần app xem ảnh/PDF trên máy.
8. Về **Account**, chọn **Log out**, rồi đăng nhập lại tài khoản cục bộ. Kết nối Dropbox của nhóm vẫn ở trên thiết bị. Chỉ **Disconnect project storage** mới xóa kết nối đó khỏi app.

Khi thử tính năng, dùng file và folder có thể xóa trong App Folder. Không dùng dữ liệu cá nhân để chụp màn hình hoặc thuyết trình.

## Giải thích ngắn khi thầy hỏi

- **Front-end / Resources:** `LoginActivity` xử lý tài khoản cục bộ. `MainActivity` có ba tab Files, Search, Account. XML layout, menu, drawable và string resource minh họa slide Android Fundamentals và Android Resources. `FileBrowserFragment` hiển thị loading/empty/error và điều hướng folder.
- **OAuth/API / Networking:** App dùng OAuth 2.0 PKCE. `SessionManager` mã hóa credential trên thiết bị. `DropboxClientFactory` tạo `DbxClientV2`; `DropboxRepository` gom các lệnh account, list, search, create, rename, delete, upload và download. `getCurrentAccount()` dùng để xác minh kết nối, không đưa tên/email Dropbox lên UI.
- **Upload/download/preview:** Android Storage Access Framework đưa cho app content URI để tải lên/xuống. Preview tải bản nhỏ vào cache riêng và dùng `FileProvider` cấp quyền đọc tạm cho viewer Android. Không cần quyền truy cập toàn bộ bộ nhớ.
- **Background work:** Network, search và đọc/ghi file chạy trên executor; kết quả quay lại main thread để cập nhật UI. `ViewModel` giữ trạng thái login, xác minh Dropbox và transfer qua xoay màn hình. Đây là phần áp dụng slide Background Tasks and Services.
- **Giới hạn:** Tài khoản cục bộ của nhiều người trên cùng thiết bị dùng chung một kết nối Dropbox. Ứng dụng tập trung vào file nhỏ trong foreground; chưa có upload session cho file lớn hay tiếp tục truyền khi process bị đóng.

## Kiểm tra trước khi trình bày

Chạy `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`, thử đăng nhập, tải thư mục, tìm kiếm, xem trước ảnh/PDF, tạo/đổi tên/xóa folder thử, upload/download một file nhỏ và kiểm tra logout. Nếu API báo thiếu quyền, xác nhận bốn scope trong App Console rồi kết nối lại Dropbox.
