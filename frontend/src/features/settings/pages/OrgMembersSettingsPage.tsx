import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi, type OrgMember } from '../../projects/api/organizationsApi';

const INVITE_ROLES = ['ADMIN', 'MEMBER', 'VIEWER'] as const;

export function OrgMembersSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<(typeof INVITE_ROLES)[number]>('MEMBER');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const membersQuery = useQuery({
    queryKey: ['org-members', org?.id],
    queryFn: () => organizationsApi.listMembers(org!.id),
    enabled: !!org?.id,
  });

  const addMember = useMutation({
    mutationFn: () => organizationsApi.addMember(org!.id, { email: email.trim(), role }),
    onSuccess: async (created) => {
      setError(null);
      setMessage(`Added ${created.displayName} as ${created.role}`);
      setEmail('');
      setRole('MEMBER');
      await queryClient.invalidateQueries({ queryKey: ['org-members', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to add member');
    },
  });

  const membershipRole = orgQuery.data?.role;
  const canInvite = membershipRole === 'OWNER' || membershipRole === 'ADMIN';
  const members = membersQuery.data ?? [];

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!email.trim() || !canInvite) return;
    addMember.mutate();
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }}>
      <Box>
        <Typography variant="h4">Organization members</Typography>
        <Typography variant="body2" color="text.secondary">
          {orgQuery.data?.name || org?.name || 'Workspace'}
          {membershipRole ? ` · your role: ${membershipRole}` : ''}
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {(membersQuery.isError || orgQuery.isError) && (
        <Alert severity="error">
          {(membersQuery.error || orgQuery.error) instanceof ApiError
            ? ((membersQuery.error || orgQuery.error) as ApiError).message
            : 'Failed to load organization members'}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: 2 }}>
        {membersQuery.isLoading ? (
          <Box sx={{ display: 'grid', placeItems: 'center', py: 4 }}>
            <CircularProgress size={28} />
          </Box>
        ) : (
          <Table size="small" data-testid="org-members-table">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {members.map((member: OrgMember) => (
                <TableRow key={member.userId} data-testid={`org-member-row-${member.userId}`}>
                  <TableCell>{member.displayName}</TableCell>
                  <TableCell>{member.email}</TableCell>
                  <TableCell>
                    <Chip size="small" label={member.role} />
                  </TableCell>
                </TableRow>
              ))}
              {members.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3}>
                    <Typography variant="body2" color="text.secondary">
                      No members found.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Paper component="form" onSubmit={onSubmit} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Add member</Typography>
          <Typography variant="body2" color="text.secondary">
            MVP invite adds an existing AI Studio account by email (they must already be registered).
          </Typography>
          {orgQuery.isSuccess && !canInvite && (
            <Alert severity="info" data-testid="org-member-invite-restricted">
              Only organization owners and admins can add members.
            </Alert>
          )}
          <TextField
            label="Email"
            type="email"
            required
            disabled={!canInvite || addMember.isPending}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'org-member-email' } }}
          />
          <FormControl fullWidth disabled={!canInvite || addMember.isPending}>
            <InputLabel id="org-member-role-label">Role</InputLabel>
            <Select
              labelId="org-member-role-label"
              label="Role"
              value={role}
              onChange={(e) => setRole(e.target.value as (typeof INVITE_ROLES)[number])}
              data-testid="org-member-role"
            >
              {INVITE_ROLES.map((r) => (
                <MenuItem key={r} value={r}>
                  {r}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            type="submit"
            variant="contained"
            disabled={!canInvite || addMember.isPending || !email.trim()}
            data-testid="org-member-add-submit"
          >
            {addMember.isPending ? 'Adding…' : 'Add member'}
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}
