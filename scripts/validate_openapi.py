from pathlib import Path
import re
import sys
import yaml
from jsonschema import Draft202012Validator, FormatChecker
from openapi_spec_validator import validate_spec

class UniqueLoader(yaml.SafeLoader):
    pass

def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError(f'Duplicate YAML key: {key}, line {key_node.start_mark.line + 1}')
        result[key] = loader.construct_object(value_node, deep=deep)
    return result

UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)
path = Path(sys.argv[1] if len(sys.argv) > 1 else Path(__file__).resolve().parents[1] / 'docs/api/condo-platform-openapi.yaml')
text = path.read_text()
assert all(line == line.rstrip() for line in text.splitlines()), 'Trailing whitespace'
assert max(map(len, text.splitlines())) < 240, 'Unexpectedly long line'
spec = yaml.load(text, Loader=UniqueLoader)
operations = []
refs = []
def visit(value):
    if isinstance(value, dict):
        if '$ref' in value:
            reference = value['$ref']
            assert reference.startswith('#/'), f'External reference: {reference}'
            resolved = spec
            for part in reference[2:].split('/'):
                resolved = resolved[part.replace('~1', '/').replace('~0', '~')]
            refs.append(reference)
        for child in value.values(): visit(child)
    elif isinstance(value, list):
        for child in value: visit(child)
visit(spec)
validate_spec(spec)
for name, item in spec['paths'].items():
    for method in ['get', 'post', 'put', 'patch', 'delete']:
        if method in item:
            operation = item[method]
            operations.append(operation['operationId'])
            parameters = item.get('parameters', []) + operation.get('parameters', [])
            declared = set()
            for parameter in parameters:
                if '$ref' in parameter:
                    parameter = spec['components']['parameters'][parameter['$ref'].split('/')[-1]]
                if parameter.get('in') == 'path': declared.add(parameter['name'])
            assert declared == set(re.findall(r'\{([^}]+)\}', name)), f'Path parameters: {method} {name}'
assert len(operations) == len(set(operations)), 'Duplicate operationId'
examples = 0
for name, schema in spec['components']['schemas'].items():
    Draft202012Validator.check_schema(schema)
    root = {'$ref': '#/components/schemas/' + name, 'components': spec['components']}
    validator = Draft202012Validator(root, format_checker=FormatChecker())
    for example in schema.get('examples', []):
        validator.validate(example)
        examples += 1
print(f'Valid OpenAPI: {len(operations)} operations, {len(spec["components"]["schemas"])} schemas, '
      f'{len(refs)} resolved references, {examples} valid schema examples.')
