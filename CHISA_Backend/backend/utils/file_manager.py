from pathlib import Path
import shutil


class FileSystemManager:
    def __init__(self, root_dir: str):
        self.root = Path(root_dir).resolve()

    def _safe_path(self, target: str) -> Path:
        """
        루트 디렉토리 밖 접근 방지
        """
        path = (self.root / target).resolve()

        if not str(path).startswith(str(self.root)):
            raise ValueError("Invalid path access")

        return path

    # =========================
    # Folder Endpoints
    # =========================

    def create_folder(self, folder_path: str) -> dict:
        """
        /folder/create
        """
        path = self._safe_path(folder_path)

        if path.exists():
            raise FileExistsError("Folder already exists")

        path.mkdir(parents=True, exist_ok=False)

        return {
            "status": "success",
            "action": "create_folder",
            "path": str(path.relative_to(self.root))
        }

    def rename_folder(self, old_path: str, new_name: str) -> dict:
        """
        /folder/rename
        """
        path = self._safe_path(old_path)

        if not path.exists() or not path.is_dir():
            raise FileNotFoundError("Folder not found")

        new_path = path.parent / new_name

        if new_path.exists():
            raise FileExistsError("Target folder already exists")

        path.rename(new_path)

        return {
            "status": "success",
            "action": "rename_folder",
            "old_path": str(path.relative_to(self.root)),
            "new_path": str(new_path.relative_to(self.root))
        }

    def move_folder(self, source_path: str, target_parent: str) -> dict:
        """
        /folder/move
        """
        source = self._safe_path(source_path)
        target = self._safe_path(target_parent)

        if not source.exists() or not source.is_dir():
            raise FileNotFoundError("Source folder not found")

        if not target.exists() or not target.is_dir():
            raise FileNotFoundError("Target folder not found")

        destination = target / source.name

        if destination.exists():
            raise FileExistsError("Destination already exists")

        shutil.move(str(source), str(destination))

        return {
            "status": "success",
            "action": "move_folder",
            "source": str(source.relative_to(self.root)),
            "destination": str(destination.relative_to(self.root))
        }

    def delete_folder(self, folder_path: str, recursive: bool = True) -> dict:
        """
        /folder/delete
        """
        path = self._safe_path(folder_path)

        if not path.exists() or not path.is_dir():
            raise FileNotFoundError("Folder not found")

        if recursive:
            shutil.rmtree(path)
        else:
            path.rmdir()

        return {
            "status": "success",
            "action": "delete_folder",
            "path": folder_path
        }

    # =========================
    # File Endpoints
    # =========================

    def delete_file(self, file_path: str) -> dict:
        """
        /file/delete
        """
        path = self._safe_path(file_path)

        if not path.exists() or not path.is_file():
            raise FileNotFoundError("File not found")

        path.unlink()

        return {
            "status": "success",
            "action": "delete_file",
            "path": file_path
        }

    def rename_file(self, file_path: str, new_name: str) -> dict:
        """
        /file/rename
        """
        path = self._safe_path(file_path)

        if not path.exists() or not path.is_file():
            raise FileNotFoundError("File not found")

        new_path = path.parent / new_name

        if new_path.exists():
            raise FileExistsError("Target file already exists")

        path.rename(new_path)

        return {
            "status": "success",
            "action": "rename_file",
            "old_path": str(path.relative_to(self.root)),
            "new_path": str(new_path.relative_to(self.root))
        }