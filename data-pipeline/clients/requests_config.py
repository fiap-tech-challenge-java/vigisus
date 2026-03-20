"""
requests_config.py
Configuração globalizada para requisições HTTP com SSL desabilitado.
Fornece uma sessão pré-configurada para ignorar verificação de certificados.
"""

import urllib3
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.ssl_ import create_urllib3_context

# Desabilitar avisos de certificado não verificado
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class SSLAdapter(HTTPAdapter):
    """Adapter HTTP que ignora completamente a verificação de certificado SSL."""
    
    def init_poolmanager(self, *args, **kwargs):
        context = create_urllib3_context()
        context.check_hostname = False
        context.verify_mode = urllib3.util.ssl_.ssl.CERT_NONE
        kwargs['ssl_context'] = context
        return super().init_poolmanager(*args, **kwargs)


def get_session_with_ssl_disabled() -> requests.Session:
    """
    Retorna uma sessão de requests com verificação SSL completamente desabilitada.
    
    :return: requests.Session configurada sem verificação de certificado.
    """
    session = requests.Session()
    session.mount('https://', SSLAdapter())
    session.mount('http://', HTTPAdapter())
    return session


# Sessão global pré-configurada
session = get_session_with_ssl_disabled()
