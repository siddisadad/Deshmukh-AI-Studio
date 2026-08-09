import {
  Alert,
  Box,
  Button,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { ssoApi, type CreateOrgSsoIdpRequest } from '../api/ssoApi';

export function SsoSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [protocol, setProtocol] = useState<'OIDC' | 'SAML'>('OIDC');
  const [slug, setSlug] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [issuerUri, setIssuerUri] = useState('');
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [metadataUrl, setMetadataUrl] = useState('');
  const [entityId, setEntityId] = useState('');
  const [acsUrl, setAcsUrl] = useState('');

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const idpsQuery = useQuery({
    queryKey: ['org-sso-idps', org?.id],
    queryFn: () => ssoApi.list(org!.id),
    enabled: !!org?.id,
  });

  const isOwner = orgQuery.data?.role === 'OWNER';

  const refresh = useMutation({
    mutationFn: (idpId: string) => ssoApi.refreshMetadata(org!.id, idpId),
    onSuccess: async () => {
      setError(null);
      setMessage('Metadata refreshed');
      await queryClient.invalidateQueries({ queryKey: ['org-sso-idps', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Metadata refresh failed');
      void queryClient.invalidateQueries({ queryKey: ['org-sso-idps', org?.id] });
    },
  });

  const create = useMutation({
    mutationFn: (body: CreateOrgSsoIdpRequest) => ssoApi.create(org!.id, body),
    onSuccess: async () => {
      setError(null);
      setMessage('SSO IdP created');
      setSlug('');
      setDisplayName('');
      await queryClient.invalidateQueries({ queryKey: ['org-sso-idps', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to create SSO IdP');
    },
  });

  const remove = useMutation({
    mutationFn: (idpId: string) => ssoApi.delete(org!.id, idpId),
    onSuccess: async () => {
      setError(null);
      setMessage('SSO IdP removed');
      await queryClient.invalidateQueries({ queryKey: ['org-sso-idps', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to delete SSO IdP');
    },
  });

  function onCreate() {
    const body: CreateOrgSsoIdpRequest = {
      slug,
      protocol,
      displayName,
      enabled: true,
    };
    if (protocol === 'OIDC') {
      body.issuerUri = issuerUri;
      body.clientId = clientId;
      body.clientSecret = clientSecret;
    } else {
      body.metadataUrl = metadataUrl;
      body.entityId = entityId;
      body.acsUrl = acsUrl;
    }
    create.mutate(body);
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }} data-testid="sso-settings">
      <Box>
        <Typography variant="h4">SSO & IdPs</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Configure multiple OIDC or SAML identity providers per organization. Metadata is
          refreshed automatically when enabled, or on demand below.
        </Typography>
        {org?.slug && (
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            Login hint: use <code>?org={org.slug}</code> on the sign-in page to show these IdPs.
          </Typography>
        )}
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      {idpsQuery.data && idpsQuery.data.length > 0 && (
        <Stack spacing={2} data-testid="sso-idp-list">
          <Typography variant="h6">Configured IdPs</Typography>
          {idpsQuery.data.map((idp) => (
            <Box key={idp.id} sx={{ py: 1.5, borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {idp.displayName}{' '}
                <Typography component="span" variant="body2" color="text.secondary">
                  ({idp.protocol} · {idp.enabled ? 'enabled' : 'disabled'})
                </Typography>
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Provider id: {`db-${idp.id}`}
              </Typography>
              {idp.metadataFetchedAt && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Metadata refreshed: {idp.metadataFetchedAt}
                </Typography>
              )}
              {idp.metadataRefreshError && (
                <Typography variant="caption" color="error" sx={{ display: 'block' }}>
                  Refresh error: {idp.metadataRefreshError}
                </Typography>
              )}
              {isOwner && (
                <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                  <Button
                    size="small"
                    onClick={() => refresh.mutate(idp.id)}
                    disabled={refresh.isPending}
                    data-testid={`sso-refresh-${idp.slug}`}
                  >
                    Refresh metadata
                  </Button>
                  <Button
                    size="small"
                    color="error"
                    onClick={() => remove.mutate(idp.id)}
                    disabled={remove.isPending}
                  >
                    Remove
                  </Button>
                </Stack>
              )}
            </Box>
          ))}
        </Stack>
      )}

      {isOwner && (
        <Stack spacing={2}>
          <Typography variant="h6">Add IdP</Typography>
          <TextField
            select
            label="Protocol"
            value={protocol}
            onChange={(e) => setProtocol(e.target.value as 'OIDC' | 'SAML')}
          >
            <MenuItem value="OIDC">OIDC</MenuItem>
            <MenuItem value="SAML">SAML</MenuItem>
          </TextField>
          <TextField label="Slug" value={slug} onChange={(e) => setSlug(e.target.value)} />
          <TextField
            label="Display name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
          {protocol === 'OIDC' ? (
            <>
              <TextField
                label="Issuer URI"
                value={issuerUri}
                onChange={(e) => setIssuerUri(e.target.value)}
              />
              <TextField
                label="Client ID"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
              />
              <TextField
                label="Client secret"
                type="password"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
              />
            </>
          ) : (
            <>
              <TextField
                label="Metadata URL"
                value={metadataUrl}
                onChange={(e) => setMetadataUrl(e.target.value)}
              />
              <TextField
                label="SP entity ID"
                value={entityId}
                onChange={(e) => setEntityId(e.target.value)}
              />
              <TextField
                label="ACS URL"
                value={acsUrl}
                onChange={(e) => setAcsUrl(e.target.value)}
              />
            </>
          )}
          <Button
            variant="contained"
            onClick={onCreate}
            disabled={create.isPending || !slug || !displayName}
            data-testid="sso-create-idp"
          >
            {create.isPending ? 'Creating…' : 'Add IdP'}
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
