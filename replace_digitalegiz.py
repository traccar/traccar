#!/usr/bin/env python3
import os
import sys

OLD_NAME = "DigitalEgiz"
NEW_NAME = "DigitalEgiz"

def replace_in_files(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for f in files:
            file_path = os.path.join(root, f)
            try:
                with open(file_path, 'r', encoding='utf-8') as fr:
                    content = fr.read()
                new_content = content.replace(OLD_NAME, NEW_NAME)
                if new_content != content:
                    with open(file_path, 'w', encoding='utf-8') as fw:
                        fw.write(new_content)
            except UnicodeDecodeError:
                # Если файл не текстовый — пропускаем
                pass
            except PermissionError as e:
                print(f"[WARNING] Нет доступа к файлу: {file_path}\nОшибка: {e}")

def rename_files_and_dirs(root_dir):
    # Обходим каталоги от глубины к поверхности (topdown=False)
    for root, dirs, files in os.walk(root_dir, topdown=False):
        # Переименовываем файлы
        for f in files:
            if OLD_NAME in f:
                old_path = os.path.join(root, f)
                new_name = f.replace(OLD_NAME, NEW_NAME)
                new_path = os.path.join(root, new_name)
                try:
                    os.rename(old_path, new_path)
                    print(f"Файл переименован:\n  {old_path} -> {new_path}")
                except PermissionError as e:
                    print(f"[WARNING] Нет доступа при переименовании файла:\n  {old_path} -> {new_path}\nОшибка: {e}")
        
        # Переименовываем каталоги
        for d in dirs:
            if OLD_NAME in d:
                old_path = os.path.join(root, d)
                new_name = d.replace(OLD_NAME, NEW_NAME)
                new_path = os.path.join(root, new_name)
                try:
                    os.rename(old_path, new_path)
                    print(f"Каталог переименован:\n  {old_path} -> {new_path}")
                except PermissionError as e:
                    print(f"[WARNING] Нет доступа при переименовании каталога:\n  {old_path} -> {new_path}\nОшибка: {e}")

def main():
    # Принимаем путь к проекту из аргументов командной строки (по умолчанию ".")
    if len(sys.argv) > 1:
        project_root = sys.argv[1]
    else:
        project_root = "."
        
    print(f"Запускаем замену содержимого файлов в каталоге: {project_root}")
    replace_in_files(project_root)
    print(f"Запускаем переименование файлов/каталогов в каталоге: {project_root}")
    rename_files_and_dirs(project_root)
    print("Готово!")

if __name__ == "__main__":
    main()
