import json


def _node_value(node, key):
    if isinstance(node, dict):
        return node[key]
    return getattr(node, key)


def build_tree(nodes, node_id=None, prefix="", is_last=True): # 이거 그냥 ../../../형식으로 해야하나...
    if node_id is None:
        node_id = next(k for k, v in nodes.items() if _node_value(v, "name") == "root")

    node = nodes[node_id]
    if prefix:
        connector = "└── " if is_last else "├── "
        line = prefix + connector + _node_value(node, "name")
    else:
        line = _node_value(node, "name")

    lines = [line]

    node_type = _node_value(node, "type")
    children = _node_value(node, "children") if isinstance(node, dict) or hasattr(node, "children") else []
    if node_type == "folder" and children:
        child_prefix = prefix + ("    " if is_last else "│   ")
        for i, child_id in enumerate(children):
            lines.extend(build_tree(nodes, child_id, child_prefix, i == len(children) - 1))

    return lines
