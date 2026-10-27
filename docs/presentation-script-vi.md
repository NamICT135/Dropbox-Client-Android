# Kịch bản trình bày Dropbox Client (5–7 phút)

## 1. Mở đầu (30 giây)

> Nhóm em xây dựng một Dropbox client cho Android bằng Java và XML. App dùng Dropbox API để xem, tìm kiếm và quản lý tệp trong App Folder đã được cấp quyền.

Nói rõ phạm vi: App Folder là vùng riêng của ứng dụng, không phải toàn bộ Dropbox của người dùng. Nhóm dùng vùng này để thử tính năng mà không đụng đến tệp cá nhân.

## 2. Ba màn hình chính (60 giây)

| Màn hình | Người dùng làm gì? | Dropbox tham gia ở đâu? |
| --- | --- | --- |
| **Account** | Tạo/đăng nhập tài khoản cục bộ; kết nối hoặc ngắt kết nối Dropbox | OAuth 2.0 PKCE xin quyền; `getCurrentAccount()` xác minh kết nối. App chỉ hiện username cục bộ và trạng thái, không hiện tên/email cá nhân hoặc Account ID Dropbox. |
| **Files** | Xem và mở folder; refresh, tạo/đổi tên/xóa folder; upload và download | `listFolder`, `listFolderContinue`, `createFolderV2`, `moveV2`, `deleteV2`, upload/download của Dropbox SDK. |
| **Search** | Tìm file/folder theo tên; mở folder trong Files hoặc mở file để tải/xem trước | `searchV2` và `searchContinueV2` tìm trên Dropbox App Folder. |

> Tài khoản cục bộ giúp nhiều bạn thử giao diện mà không cần dùng email cá nhân. Kết nối Dropbox là một phiên riêng trên từng thiết bị. Các tài khoản cục bộ trên cùng thiết bị dùng chung kết nối đó; đây không phải hệ thống lưu trữ riêng cho từng người.

## 3. Sơ đồ luồng kỹ thuật (60 giây)

```text
Nút trên màn hình (Activity / Fragment, XML)
    -> Executor chạy tác vụ nền
        -> DropboxRepository
            -> DbxClientV2 (Dropbox SDK)
                -> Dropbox API / App Folder
    <- Kết quả về main thread để cập nhật UI
```

> `SessionManager` giữ credential Dropbox được mã hóa bằng Android Keystore. `DropboxClientFactory` tạo client SDK từ phiên đó. UI gọi qua repository thay vì rải lệnh SDK khắp các màn hình. Network và đọc/ghi tệp chạy ngoài main thread, nên màn hình vẫn phản hồi và hiện trạng thái loading/error. Đây là cách nhóm áp dụng các phần Activity/Fragment, Android Resources, Background Tasks và Networking trong slide môn học.

## 4. Thao tác trực tiếp trên app (2–3 phút)

Chuẩn bị một folder và một file ảnh/PDF nhỏ, có thể xóa, trong App Folder. Không dùng file cá nhân trên màn hình chiếu.

1. Mở app, đăng nhập bằng tài khoản cục bộ. Vào **Account** và chỉ trạng thái **Connected to Dropbox**. Giải thích: tên và email Dropbox được ẩn trên UI; kết nối vẫn được API xác minh.
2. Vào **Files**, mở folder con, bấm **Up** để quay lại, bấm **Refresh**. Chỉ trạng thái loading, danh sách tệp và trạng thái folder rỗng nếu có.
3. Tạo folder thử bằng **Create folder**. Mở menu cạnh folder để **Rename** rồi **Delete**; cho thấy hộp thoại xác nhận. Các thay đổi này được gửi lên Dropbox.
4. Vào **Search**, nhập một phần tên file, bấm **Search Dropbox**. Chạm kết quả folder để chuyển về Files hoặc chạm file để mở màn hình truyền tệp.
5. Với file ảnh/PDF nhỏ, bấm **Preview**. App tải bản xem trước vào cache riêng rồi cấp quyền đọc tạm cho viewer Android. Với file khác, bấm **Download from Dropbox** và chọn nơi lưu bằng Android file picker.
6. Trong **Files**, bấm **Upload file** và chọn file thử bằng Android picker. Quay lại danh sách để thấy file vừa được tải lên. Sau đó tải file về và đối chiếu nội dung.
7. Vào **Account → Log out**. Đăng nhập lại tài khoản cục bộ và chỉ ra kết nối Dropbox trên thiết bị vẫn còn. **Disconnect project storage** mới xóa phiên Dropbox của app.

Nếu mạng yếu, ưu tiên bước 1–4 và dùng file nhỏ. Không khẳng định một thao tác API đã thành công nếu app còn hiện loading hoặc lỗi.

## 5. Câu hỏi thầy có thể hỏi (60–90 giây)

**“Dữ liệu trong app có đến từ Dropbox không?”** Files và Search lấy metadata qua Dropbox API. Upload/download dùng `DbxClientV2`; nhóm kiểm tra tên, đường dẫn và nội dung tệp sau khi API trả kết quả. Account gọi `getCurrentAccount()` để kiểm tra kết nối.

**“Tại sao không chạy API trên main thread?”** Gọi mạng hoặc đọc file có thể chậm và làm app đứng. Nhóm dùng executor cho công việc nền, rồi đưa kết quả về main thread để cập nhật View.

**“Tại sao app không hiện email Dropbox?”** Email không cần cho thao tác file. App chỉ hiện trạng thái kết nối để tránh lộ thông tin cá nhân khi nhiều thành viên dùng chung thiết bị hoặc trình chiếu.

**“Login cục bộ có phải Dropbox login không?”** Không. Login cục bộ chỉ mở app trên thiết bị. Dropbox OAuth là bước cấp quyền riêng cho API. Vì vậy đổi username cục bộ không đổi tài khoản Dropbox đang kết nối.

**“Tại sao không giống toàn bộ app Dropbox chính thức?”** [Dropbox Mobile](https://www.dropbox.com/mobile) giới thiệu truy cập tệp và xem trước nội dung. Nhóm chọn Preview từ hướng đó và thêm Search theo kế hoạch nhóm, vì cả hai phù hợp phạm vi App Folder và kiến thức môn học. Chia sẻ link cần scope bổ sung; scan, sao lưu ảnh và đồng bộ offline cần kiến trúc, quyền và kiểm thử riêng, nên chưa đưa vào bài này.

## 6. Kết luận (20 giây)

> Nhóm đã kết nối giao diện Android với Dropbox qua OAuth và SDK. Phần demo cho thấy app đọc và thay đổi tệp trong App Folder, đồng thời báo trạng thái khi đang tải hoặc gặp lỗi.

Trước buổi trình bày, chạy lại đúng phiên bản PR đã merge và thử toàn bộ bước trên một thiết bị được kết nối **tài khoản Dropbox của nhóm**. Search có thể mất ít thời gian để thấy file vừa tải lên do Dropbox lập chỉ mục; nếu vậy dùng **Files → Refresh** để kiểm tra file ngay.
