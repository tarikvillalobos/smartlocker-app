from pathlib import Path
import yaml
from jsonschema import Draft202012Validator, FormatChecker

spec = yaml.safe_load((Path(__file__).resolve().parents[1] / 'docs/api/openapi.yaml').read_text())
login = {'contact': '+5511987654321', 'cpf': '52998224725', 'channel': 'sms'}
metrics = {'since': '2026-08-27T12:00:00.000Z', 'until': '2026-09-26T12:00:00.000Z',
           'generatedAt': '2026-09-26T12:00:00.000Z', 'complete': True,
           'totalReceived': 8, 'physicalPickupCount': 6, 'averagePickupDurationSeconds': 9900}
capabilities = {
 'features': {key: False for key in ['manualPickup', 'undoManualPickup', 'contactEditing',
                                    'recipients', 'supportIssues', 'pushRegistration']},
 'channels': {key: {'available': False} for key in ['inApp', 'sms', 'email', 'whatsapp', 'push']},
}
standalone = {'id': 'membership', 'locationId': 'station', 'locationName': 'Estação Central',
              'unitId': None, 'unitLabel': None, 'timeZone': 'America/Sao_Paulo', 'capabilities': capabilities}
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
 ('OtpVerification', {'code': '12345'}, False),
 ('PreferencesUpdate', {}, False),
 ('PreferencesUpdate', {'sms': False}, True),
 ('PreferencesUpdate', {'camera': True}, False),
 ('ParcelMetrics', metrics, True),
 ('ParcelMetrics', dict(metrics, complete=False), False),
 ('ParcelMetrics', dict(metrics, physicalPickupCount=0), False),
 ('ParcelMetrics', dict(metrics, complete=False, totalReceived=None,
                        physicalPickupCount=None, averagePickupDurationSeconds=None), True),
 ('Instant', '2026-09-26T12:00:00.000Z', True),
 ('Instant', '2026-09-26T09:00:00-03:00', False),
 ('PushRegistrationRequest', {'platform': 'android', 'provider': 'apns',
    'token': 'synthetic-token', 'permission': 'authorized', 'appVersion': '0.1.0'}, False),
]
for name, value, expected in cases:
    root = {'$ref': '#/components/schemas/' + name, 'components': spec['components']}
    actual = Draft202012Validator(root, format_checker=FormatChecker()).is_valid(value)
    assert actual == expected, f'Unexpected validity for {name}'
print(f'{len(cases)} positive and negative schema cases passed.')
