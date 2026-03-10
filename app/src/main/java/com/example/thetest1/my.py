import os
import shutil
from pathlib import Path

def convert_kt_to_txt():
    # Вказуємо, куди перекидати файли (наприклад, в папку output)
    output_folder = "output"
    
    # Створюємо папку для результатів, якщо ще не існує
    os.makedirs(output_folder, exist_ok=True)

    # Поточна папка (або будь-яка вкладена)
    current_dir = Path.cwd()

    # Збираємо всі .kt файли (рекурсивно)
    kt_files = list(current_dir.rglob("*.kt"))

    print(f"Знайдено {len(kt_files)} файлів з розширенням .kt")

    # Перетворюємо кожен .kt у .txt і перекидуємо в output/
    for kt_file in kt_files:
        # Отримуємо шлях до відповідного .txt файлу
        txt_file_path = kt_file.with_suffix(".txt")
        
        # Створюємо шлях до вихідної папки (без вкладень)
        relative_path = kt_file.relative_to(current_dir)
        # Отримуємо папку для цього файлу (наприклад: folder/subfolder/file.kt -> folder/subfolder/file.txt)
        # Але ми хочемо все в output/
        # Тому видаляємо вкладення та робимо тільки output/
        
        # Просто зберігаємо у output/ з ім'ям файлу
        # Наприклад: output/file.txt
        txt_output_path = Path(output_folder) / txt_file_path.name
        
        # Перетворюємо вміст
        try:
            with open(kt_file, 'r', encoding='utf-8') as kt_in:
                content = kt_in.read()
            with open(txt_output_path, 'w', encoding='utf-8') as txt_out:
                txt_out.write(content)
            print(f"Перетворено: {kt_file.name} -> {txt_output_path.name}")
        except Exception as e:
            print(f"Помилка при обробці {kt_file.name}: {e}")

if name == "main":
    convert_kt_to_txt()