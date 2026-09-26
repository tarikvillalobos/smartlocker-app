from pathlib import Path
import yaml
from jsonschema import Draft202012Validator, FormatChecker

spec = yaml.safe_load((Path(__file__).resolve().parents[1] / 'docs/api/openapi.yaml').read_text())
login = {'contact': '+5511987654321', 'cpf': '52998224725', 'channel': 'sms'}
metrics = {'since': '2026-08-27T12:00:00.000Z', 'until': '2026-09-26T12:00:00.000Z',
           'generatedAt': '2026-09-26T12:00:00.000Z', 'complete': True,
           'totalReceived': 8, 'physicalPickupCount': 6, 'averagePickupDurationSeconds': 9900}
cases = [
 ('LoginRequest', login, True),
 ('LoginRequest', dict(login, contact='ana@example.test', channel='email'), True),
 ('LoginRequest', dict(login, cpf='11111111111'), False),
 ('LoginRequest', dict(login, cpf='529.982.247-25'), False),
 ('LoginRequest', dict(login, contact='11987654321'), False),
 ('LoginRequest', dict(login, contact='ana@example.test'), False),
 ('LoginRequest', dict(login, contact='ana@', channel='email'), False),
 ('ContactChangeRequest', {'contact': 'new@example.test', 'channel': 'email'}, True),
 ('ContactChangeRequest', {'contact': '', 'channel': 'email'}, False),
 ('OtpVerification', {'code': '123456'}, True),
