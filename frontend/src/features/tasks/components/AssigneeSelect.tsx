import { FormControl, FormHelperText, InputLabel, MenuItem, Select, type SelectChangeEvent } from '@mui/material';
import type { OrgMember } from '../../projects/api/organizationsApi';

interface Props {
  members: OrgMember[];
  value: string;
  onChange: (assigneeId: string) => void;
  label?: string;
  testId?: string;
}

/** Empty string means unassigned; option values are user ids. */
export function AssigneeSelect({
  members,
  value,
  onChange,
  label = 'Assignee',
  testId = 'assignee-select',
}: Props) {
  function handleChange(event: SelectChangeEvent<string>) {
    onChange(event.target.value);
  }

  return (
    <FormControl fullWidth>
      <InputLabel id={`${testId}-label`}>{label}</InputLabel>
      <Select
        labelId={`${testId}-label`}
        label={label}
        value={value}
        onChange={handleChange}
        data-testid={testId}
      >
        <MenuItem value="" data-testid={`${testId}-none`}>
          Unassigned
        </MenuItem>
        {members.map((member) => (
          <MenuItem key={member.userId} value={member.userId} data-testid={`${testId}-option-${member.userId}`}>
            {member.displayName} ({member.email})
          </MenuItem>
        ))}
      </Select>
      {members.length === 0 && (
        <FormHelperText data-testid={`${testId}-empty`}>No organization members available</FormHelperText>
      )}
    </FormControl>
  );
}
