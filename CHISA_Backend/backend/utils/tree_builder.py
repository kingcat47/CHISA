import json


def build_tree(nodes, node_id=None, prefix="", is_last=True):
    if node_id is None:
        node_id = next(k for k, v in nodes.items() if v["name"] == "root")

    node = nodes[node_id]
    if prefix:
        connector = "└── " if is_last else "├── "
        line = prefix + connector + node["name"]
    else:
        line = node["name"]

    lines = [line]

    if node["type"] == "folder" and "children" in node:
        children = node["children"]
        child_prefix = prefix + ("    " if is_last else "│   ")
        for i, child_id in enumerate(children):
            lines.extend(build_tree(nodes, child_id, child_prefix, i == len(children) - 1))

    return lines


if __name__ == "__main__":
    path = "/Users/sungho/dev/CHISA_Backend/file_example.json"
    with open(path, "r") as f:
        data = json.load(f)
    result = build_tree(data["nodes"])
    print("\n".join(result))
