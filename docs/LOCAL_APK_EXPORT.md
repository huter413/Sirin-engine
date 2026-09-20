# Yerel APK dışa aktarma

Şirin Engine proje açıldıktan sonra **APK DERLE** düğmesini gösterir ve GitHub/uzak CI gerektirmeyen yerel dışa aktarma katmanını kullanır.

## Download kaydı

- Android 10+ (API 29+) için `MediaStore.Downloads` kullanılır.
- `RELATIVE_PATH=Download` ile çıktı sistemin Download alanına yazılır.
- `IS_PENDING` ile yazma tamamlanana kadar dosya yarım çıktı olarak görünmez.
- Android 9 ve altı için yalnızca gerekli eski depolama izni istenir ve klasik Download klasörü kullanılır.

## APK derleyicisi

Bu katman gerçek bir APK dosyası verilmeden sahte APK üretmez. Projeyi gerçek Android uygulamasına dönüştürmek için Şirin Engine'e ayrıca bir Android runtime/export compiler eklenmesi gerekir. Bu repo değişikliği dosya kaydetme katmanını gerçek Android API'lerine taşır; ZIP'i APK diye yeniden adlandırmaz.
