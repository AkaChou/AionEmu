import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("generate_retail_instance_data.py")
SPEC = importlib.util.spec_from_file_location("retail_instance_data", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GENERATOR)


class InstanceDataSourceTest(unittest.TestCase):

    def test_java_world_references_are_repo_relative(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            relative = Path("src/main/java/example")
            source = root / relative / "PortalAI2.java"
            source.parent.mkdir(parents=True)
            source.write_text("class PortalAI2 { int world = 300460000; }\n", encoding="utf-8")

            self.assertEqual(
                {300460000: relative / source.name},
                GENERATOR.java_world_references(root, relative, {300460000}),
            )

    def test_absolute_behavior_sources_are_rejected(self) -> None:
        GENERATOR.validate_behavior_source("src/main/java/example/PortalAI2.java", 300460000)
        with self.assertRaisesRegex(ValueError, "absolute behavior_source"):
            GENERATOR.validate_behavior_source("/Users/example/PortalAI2.java", 300460000)

    def test_dimension_owner_is_replaced_in_place(self) -> None:
        self.assertEqual(
            "entry:RETAIL_PORTAL,door:RETAIL_PATTERN,recovery:STATELESS",
            GENERATOR.replace_dimension_owner(
                "entry:RETAIL_PORTAL,door:RETAIL_DATA,recovery:STATELESS", "door", "RETAIL_PATTERN"),
        )
        with self.assertRaisesRegex(ValueError, "invalid door dimension owner"):
            GENERATOR.replace_dimension_owner("entry:RETAIL_PORTAL", "door", "RETAIL_PATTERN")

    def test_handler_path_owner_follows_inherited_capability(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            handlers = root / "src/main/java/com/aionemu/gameserver/instance/handlers/scripts"
            handlers.mkdir(parents=True)
            (handlers / "Moving.java").write_text(
                "class Moving { void start() { TeleportService.teleport(); } }\n", encoding="utf-8")
            (handlers / "Child.java").write_text("class Child extends Moving {}\n", encoding="utf-8")
            (handlers / "Static.java").write_text("class Static {}\n", encoding="utf-8")

            owners = GENERATOR.handler_path_owners(root, {
                1: (handlers / "Child.java").relative_to(root),
                2: (handlers / "Static.java").relative_to(root),
            })

        self.assertEqual({1: "HANDLER", 2: "RUNTIME_PATHING"}, owners)


if __name__ == "__main__":
    unittest.main()
