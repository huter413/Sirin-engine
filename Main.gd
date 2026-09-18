extends Control

var title := "SIRIN ENGINE"
var status := "Hazır"
var zip_name := "Proje ZIP seçilmedi"

func _ready() -> void:
	get_viewport().size_changed.connect(_on_viewport_changed)
	build_ui()
	_on_viewport_changed()

func _on_viewport_changed() -> void:
	# Keep the editor intentionally landscape on mobile.
	DisplayServer.window_set_size(Vector2i(1280, 720))

func build_ui() -> void:
	var bg := ColorRect.new()
	bg.color = Color("#11151c")
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	var header := Label.new()
	header.text = title
	header.position = Vector2(40, 28)
	header.add_theme_font_size_override("font_size", 34)
	add_child(header)

	var sub := Label.new()
	sub.text = "Mobil dokunmatik • Yatay ekran • ZIP → APK proje yöneticisi"
	sub.position = Vector2(42, 76)
	sub.add_theme_font_size_override("font_size", 18)
	add_child(sub)

	var zip_button := Button.new()
	zip_button.text = "PROJE ZIP SEÇ"
	zip_button.position = Vector2(50, 150)
	zip_button.size = Vector2(330, 82)
	zip_button.add_theme_font_size_override("font_size", 24)
	zip_button.pressed.connect(_choose_zip)
	add_child(zip_button)

	var build_button := Button.new()
	build_button.text = "APK DERLE"
	build_button.position = Vector2(410, 150)
	build_button.size = Vector2(330, 82)
	build_button.add_theme_font_size_override("font_size", 24)
	build_button.pressed.connect(_build_apk)
	add_child(build_button)

	var settings := Label.new()
	settings.text = "Çıkış: Android APK\nEkran: LANDSCAPE\nDokunmatik kontroller: AÇIK\nGrafik: Mobile/Compatibility"
	settings.position = Vector2(50, 280)
	settings.add_theme_font_size_override("font_size", 20)
	add_child(settings)

	var info := Label.new()
	info.name = "Status"
	info.text = "Durum: " + status + "\n" + zip_name
	info.position = Vector2(50, 430)
	info.add_theme_font_size_override("font_size", 20)
	add_child(info)

	var note := Label.new()
	note.text = "Not: APK derleme için GitHub Actions workflow'u hazırdır.\nTelefonu kasıtlı olarak aşırı ısıtan/stresleyen bir yük eklenmez."
	note.position = Vector2(50, 560)
	note.add_theme_font_size_override("font_size", 16)
	add_child(note)

func _choose_zip() -> void:
	status = "ZIP seçimi bu arayüz için hazırlandı"
	_update_status()

func _build_apk() -> void:
	status = "APK build workflow'una gönderilmeye hazır"
	_update_status()

func _update_status() -> void:
	var label := get_node_or_null("Status") as Label
	if label:
		label.text = "Durum: " + status + "\n" + zip_name
